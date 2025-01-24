

from flask import Flask, request, jsonify
from flask_cors import CORS
import openai
from openai import OpenAI
from dotenv import load_dotenv
import logging
import os
from werkzeug.utils import secure_filename
import PyPDF2
import docx
import uuid
from collections import defaultdict
from dotenv import load_dotenv

# Initialize Flask app
app = Flask(__name__)
CORS(app)
load_dotenv()  # Load variables from .env file

# Initialize OpenAI API client
openai.api_key = os.getenv("OPENAI_API_KEY")
client = OpenAI()
# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Allowed file extensions and upload folder
ALLOWED_EXTENSIONS = {"pdf", "docx", "txt"}
UPLOAD_FOLDER = "uploads"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
app.config["UPLOAD_FOLDER"] = UPLOAD_FOLDER

# Storage
courses = {}  # {course_id: {"name": str, "content": str}}
student_chats = defaultdict(lambda: defaultdict(list))  # {student_id: {course_id: [messages]}}

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
    """Upload course content."""
    if "file" not in request.files or "course_name" not in request.form:
        return jsonify({"error": "File and course_name are required"}), 400

    file = request.files["file"]
    course_name = request.form["course_name"]

    if not file or not allowed_file(file.filename):
        return jsonify({"error": "Invalid file type"}), 400

    # Secure filename and save file
    filename = secure_filename(file.filename)
    filepath = os.path.join(app.config["UPLOAD_FOLDER"], filename)
    file.save(filepath)
    logger.info(f"File uploaded: {filename}")

    # Extract content
    if filename.endswith(".pdf"):
        content = extract_text_from_pdf(filepath)
    elif filename.endswith(".docx"):
        content = extract_text_from_docx(filepath)
    elif filename.endswith(".txt"):
        with open(filepath, "r") as f:
            content = f.read()
    else:
        content = ""

    if not content.strip():
        return jsonify({"error": "Failed to extract text from the document"}), 500

    # Store course
    course_id = str(uuid.uuid4())
    courses[course_id] = {"name": course_name, "content": content}
    return jsonify({"message": "Course uploaded successfully", "course_id": course_id}), 200

# @app.route("/student/start_chat", methods=["POST"])
# def start_chat():
#     """Start a chat for a course."""
#     data = request.get_json()
#     if not data or "student_id" not in data or "course_id" not in data:
#         return jsonify({"error": "student_id and course_id are required"}), 400

#     student_id = data["student_id"]
#     course_id = data["course_id"]

#     if course_id not in courses:
#         return jsonify({"error": "Invalid course_id"}), 404

#     student_chats[student_id][course_id] = []  # Initialize chat
#     return jsonify({"message": "Chat started successfully", "course_name": courses[course_id]["name"]}), 200

@app.route("/student/chat", methods=["POST"])
def student_chat():
    """Send a message to the course AI."""
    data = request.get_json()
    if not data or "student_id" not in data or "course_id" not in data or "message" not in data:
        return jsonify({"error": "student_id, course_id, and message are required"}), 400

    student_id = data["student_id"]
    course_id = data["course_id"]
    message = data["message"]

    if course_id not in courses:
        print(course_id)
        print(courses)
        #print("hello")
        return jsonify({"error": "Invalid course_id"}), 404

    chat_history = student_chats[student_id][course_id]
    chat_history.append({"role": "user", "content": message})

    try:
        response = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": f"Use the following course content to answer student queries:\n{courses[course_id]['content'][:4000]}"},
                *chat_history
            ],
            max_tokens=100  # Limit the response to a maximum of 300 tokens
        )
        # print(response)
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

    if course_id not in courses or course_id not in student_chats[student_id]:
        return jsonify({"error": "No chat history found for this course"}), 404

    return jsonify({"chat_history": student_chats[student_id][course_id]}), 200

@app.route("/student/courses", methods=["GET"])
def get_courses():
    """Retrieve all uploaded courses."""
    if not courses:
        return jsonify({"error": "No courses available"}), 404

    course_list = {course_id: course_data["name"] for course_id, course_data in courses.items()}
    return jsonify(course_list), 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
