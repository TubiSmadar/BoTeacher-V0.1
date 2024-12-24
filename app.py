from flask import Flask, request, jsonify
from flask_cors import CORS
import openai
import logging
import os
from werkzeug.utils import secure_filename
import PyPDF2
import docx
import json
import uuid
from collections import defaultdict

# Initialize Flask app
app = Flask(__name__)
CORS(app)

# Replace with your OpenAI API key
openai.api_key = "secret"

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Allowed file extensions
ALLOWED_EXTENSIONS = {"pdf", "docx", "txt"}
UPLOAD_FOLDER = "uploads"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
app.config["UPLOAD_FOLDER"] = UPLOAD_FOLDER

# Storage
courses = {}  # {course_id: {"name": str, "content": str}}
student_chats = defaultdict(lambda: defaultdict(list))  # {student_id: {course_id: [messages]}}

# Helper Functions
def allowed_file(filename):
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS

def extract_text_from_pdf(filepath):
    text = ""
    try:
        with open(filepath, "rb") as pdf_file:
            reader = PyPDF2.PdfReader(pdf_file)
            for page in reader.pages:
                text += page.extract_text() or ""
    except Exception as e:
        logger.error(f"Error extracting text from PDF: {e}")
    return text

def extract_text_from_docx(filepath):
    text = ""
    try:
        doc = docx.Document(filepath)
        for paragraph in doc.paragraphs:
            text += paragraph.text + "\n"
    except Exception as e:
        logger.error(f"Error extracting text from DOCX: {e}")
    return text

# Routes
@app.route("/lecturer/upload_course", methods=["POST"])
def upload_course():
    """Lecturer uploads course content."""
    if "file" not in request.files or "course_name" not in request.form:
        print("error1")
        return jsonify({"error": "File and course_name are required"}), 400

    file = request.files["file"]
    course_name = request.form["course_name"]

    if file.filename == "" or not allowed_file(file.filename):

        return jsonify({"error": "Invalid file type"}), 400

    # Save the file
    filename = secure_filename(file.filename)
    filepath = os.path.join(app.config["UPLOAD_FOLDER"], filename)
    file.save(filepath)
    logger.info(f"File uploaded: {filename}")

    # Extract content
    content = ""
    if filename.endswith(".pdf"):
        content = extract_text_from_pdf(filepath)
    elif filename.endswith(".docx"):
        content = extract_text_from_docx(filepath)
    elif filename.endswith(".txt"):
        with open(filepath, "r") as f:
            content = f.read()

    if not content.strip():
        return jsonify({"error": "Failed to extract text from the document"}), 500

    # Store course
    course_id = str(uuid.uuid4())
    courses[course_id] = {"name": course_name, "content": content}

    return jsonify({"message": "Course uploaded successfully", "course_id": course_id}), 200

@app.route("/student/start_chat", methods=["POST"])
def start_chat():
    """Student starts a chat for a course."""
    data = request.get_json()
    if not data or "student_id" not in data or "course_id" not in data:
        return jsonify({"error": "student_id and course_id are required"}), 400

    student_id = data["student_id"]
    course_id = data["course_id"]

    if course_id not in courses:
        return jsonify({"error": "Invalid course_id"}), 404

    student_chats[student_id][course_id] = []  # Initialize chat

    return jsonify({"message": "Chat started successfully", "course_name": courses[course_id]["name"]}), 200


@app.route("/student/chat", methods=["POST"])
def student_chat():
    """Student sends a message to the course AI."""
    data = request.get_json()
    if not data or "student_id" not in data or "course_id" not in data or "message" not in data:
        return jsonify({"error": "student_id, course_id, and message are required"}), 400

    student_id = data["student_id"]
    course_id = data["course_id"]
    message = data["message"]

    if course_id not in courses:
        print('what?')
        return jsonify({"error": "Invalid course_id"}), 404

    # Load previous chat history
    chat_history = student_chats[student_id][course_id]

    # Append user message to history
    chat_history.append({"role": "user", "content": message})

    # Generate response using OpenAI API
    try:
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": f"Use the following course content to answer student queries:\n{courses[course_id]['content'][:4000]}"}
            ] + chat_history
        )
        reply = response['choices'][0]['message']['content']
        chat_history.append({"role": "assistant", "content": reply})
    except openai.error.OpenAIError as e:
        logger.error(f"OpenAI API error: {str(e)}")
        return jsonify({"error": "Failed to generate a response. Try again later."}), 500

    return jsonify({"response": reply}), 200

@app.route("/student/get_chat", methods=["POST"])
def get_chat():
    """Retrieve a student's chat history for a course."""
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
