from flask import Flask, request, jsonify
from flask_cors import CORS
import openai
from dotenv import load_dotenv
import logging
import os
from werkzeug.utils import secure_filename
import PyPDF2
import docx
import uuid
from collections import defaultdict

# --- Firebase Admin imports ---
import firebase_admin
from firebase_admin import credentials, firestore

# Initialize Flask app
app = Flask(__name__)
CORS(app)
load_dotenv()  # Load variables from .env file

# Initialize Firebase Admin SDK and Firestore
# Expect FIREBASE_SERVICE_ACCOUNT_PATH to be defined in your .env file
cred_path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
if not cred_path:
    raise Exception("FIREBASE_SERVICE_ACCOUNT_PATH environment variable is not set.")
cred = credentials.Certificate(cred_path)
firebase_admin.initialize_app(cred)
db = firestore.client()

# Initialize OpenAI API client
openai.api_key = os.getenv("OPENAI_API_KEY")
# We now use openai.ChatCompletion.create() later—no separate client object is needed

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Allowed file extensions and upload folder
ALLOWED_EXTENSIONS = {"pdf", "docx", "txt"}
UPLOAD_FOLDER = "uploads"
USERS_COLLECTION = "Users"
COURSES_COLLECTION = "enrolledCourses"
FILES_COLLECTION = "files"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
app.config["UPLOAD_FOLDER"] = UPLOAD_FOLDER

# In-memory storage for student chats
# We still use in-memory storage for chat history.
student_chats = defaultdict(lambda: defaultdict(list))
# Note: For production, consider persisting chats in Firestore as well.

# Helper Functions
def allowed_file(filename):
    """Check if file extension is allowed."""
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS

def extract_text_from_pdf(filepath):
    """Extract text from a PDF file."""
    try:
        with open(filepath, "rb") as pdf_file:
            reader = PyPDF2.PdfReader(pdf_file)
            return "".join(page.extract_text() or "" for page in reader.pages)
    except Exception as e:
        logger.error(f"Error extracting text from PDF: {e}")
        return ""

def extract_text_from_docx(filepath):
    """Extract text from a DOCX file."""
    try:
        doc = docx.Document(filepath)
        return "\n".join(paragraph.text for paragraph in doc.paragraphs)
    except Exception as e:
        logger.error(f"Error extracting text from DOCX: {e}")
        return ""

# Routes
@app.route("/lecturer/upload_course", methods=["POST"])
def upload_course():
    """Upload course content to Firestore."""
    if "file" not in request.files or "file_name" not in request.form:
        return jsonify({"error": "File and file_name are required"}), 400

    file = request.files["file"]
    file_name = request.form["file_name"]

    if not file or not allowed_file(file.filename):
        return jsonify({"error": "Invalid file type"}), 400

    # Secure filename and save file locally
    filename = secure_filename(file.filename)
    filepath = os.path.join(app.config["UPLOAD_FOLDER"], filename)
    file.save(filepath)
    logger.info(f"File uploaded: {filename}")

    # Extract content from file
    if filename.endswith(".pdf"):
        content = extract_text_from_pdf(filepath)
    elif filename.endswith(".docx"):
        content = extract_text_from_docx(filepath)
    elif filename.endswith(".txt"):
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
    else:
        content = ""

    if not content.strip():
        return jsonify({"error": "Failed to extract text from the document"}), 500

    # Store course in Firestore
    file_id = str(uuid.uuid4())
    file_data = {"name": file_name, "content": content}
    try:
        db.collection(FILES_COLLECTION).document(file_id).set(file_data)
    except Exception as e:
        logger.error(f"Error saving file to Firestore: {e}")
        return jsonify({"error": "Failed to store file data"}), 500

    return jsonify({"message": "Course uploaded successfully", "file_id": file_id}), 200

@app.route("/student/chat", methods=["POST"])
def student_chat():
    """Send a message to the course AI."""
    data = request.get_json()
    if not data or "student_id" not in data or "course_id" not in data or "message" not in data:
        return jsonify({"error": "student_id, course_id, and message are required"}), 400

    student_id = data["student_id"]
    course_id = data["course_id"]
    message = data["message"]

    # Step 1: Query users collection to get the enrolled course document
    user_ref = db.collection(USERS_COLLECTION).document(student_id)
    enrolled_course_ref = user_ref.collection(COURSES_COLLECTION).document(course_id)
    enrolled_course_doc = enrolled_course_ref.get()
    if not enrolled_course_doc.exists:
        logger.error(f"Student {student_id} is not enrolled in course {course_id}")
        return jsonify({"error": "Invalid course enrollment"}), 404

    # Step 2: Get the course reference from the enrolledCourses document
    enrolled_course_data = enrolled_course_doc.to_dict()
    course_ref = enrolled_course_data.get("courseRef")
    if not course_ref:
        logger.error(f"No courseRef found for course {course_id}")
        return jsonify({"error": "Invalid course reference"}), 404

    # Step 3: Get the actual course document using the courseRef
    course_doc = course_ref.get()
    if not course_doc.exists:
        logger.error(f"Invalid course reference: {course_ref.id}")
        return jsonify({"error": "Invalid course reference"}), 404

    course_data = course_doc.to_dict()

    # Step 4: Get the files array from the course document
    file_docs = course_data.get("files", [])
    if not file_docs:
        logger.error("No files found for the course.")
        return jsonify({"error": "No course content available"}), 404

    # Step 5: Loop over file_docs to get file data from FILES_COLLECTION
    course_data_content = []
    for file_id in file_docs:
        file_ref = db.collection(FILES_COLLECTION).document(file_id)
        file_doc = file_ref.get()
        if file_doc.exists:
            file_data = file_doc.to_dict().get("content", "")
            course_data_content.append(file_data)
        else:
            logger.warning(f"File with ID {file_id} not found.")

    # Combine all file contents
    course_data = "\n".join(course_data_content)

    '''
    # Retrieve course content from Firestore
    course_ref = db.collection(FILES_COLLECTION).document(course_id)
    course_doc = course_ref.get()
    if not course_doc.exists:
        print(db.collection(FILES_COLLECTION))
        logger.error(f"Invalid course_id: {course_id}")
        return jsonify({"error": "Invalid course_id"}), 404

    course_data = course_doc.to_dict()


    # Record student's message in in-memory chat history
    chat_history = student_chats[student_id][course_id]
    chat_history.append({"role": "user", "content": message})
    '''

    # Record student's message in in-memory chat history
    if student_id not in student_chats:
        student_chats[student_id] = {}
    if course_id not in student_chats[student_id]:
        student_chats[student_id][course_id] = []

    chat_history = student_chats[student_id][course_id]
    chat_history.append({"role": "user", "content": message})

    try:
        # Use OpenAI ChatCompletion API to generate a response
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": f"Use the following course content to answer student queries:\n{course_data[:4000]}"}, #changed from course_data['content']
                *chat_history
            ],
            max_tokens=1500  # Adjust token limit as needed
        )
        reply = response.choices[0].message.content
        chat_history.append({"role": "assistant", "content": reply})
    except openai.error.OpenAIError as e:
        logger.error(f"OpenAI API error: {e}")
        return jsonify({"error": "Failed to generate a response. Try again later."}), 500

    return jsonify({"response": reply}), 200

@app.route("/student/get_chat", methods=["POST"])
def get_chat():
    """Retrieve chat history for a course."""
    data = request.get_json()
    if not data or "student_id" not in data or "course_id" not in data:
        return jsonify({"error": "student_id and course_id are required"}), 400

    student_id = data["student_id"]
    course_id = data["course_id"]

    # Check if course exists in Firestore
    user_ref = db.collection(USERS_COLLECTION).document(student_id)
    course_ref = user_ref.collection(COURSES_COLLECTION).document(course_id)
    if not course_ref.get().exists or course_id not in student_chats[student_id]:
        return jsonify({"error": "No chat history found for this course"}), 404

    return jsonify({"chat_history": student_chats[student_id][course_id]}), 200

@app.route("/student/courses", methods=["GET"])
def get_courses():
    """Retrieve all uploaded courses from Firestore."""
    try:
        docs = db.collection(FILES_COLLECTION).stream()
        course_list = {doc.id: doc.to_dict().get("name") for doc in docs}
    except Exception as e:
        logger.error(f"Error retrieving courses: {e}")
        return jsonify({"error": "Failed to retrieve courses"}), 500

    if not course_list:
        return jsonify({"error": "No courses available"}), 404

    return jsonify(course_list), 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
