from flask import Flask, request, jsonify
from flask_cors import CORS
import openai
import logging
import os
from werkzeug.utils import secure_filename
import PyPDF2
import docx

# Initialize Flask app
app = Flask(__name__)
CORS(app)

# Replace with your OpenAI API key
openai.api_key = ""
# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Storage for uploaded content
uploaded_content = ""

# Allowed file extensions
ALLOWED_EXTENSIONS = {"pdf", "docx"}
UPLOAD_FOLDER = "uploads"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
app.config["UPLOAD_FOLDER"] = UPLOAD_FOLDER

def allowed_file(filename):
    """Check if the file has an allowed extension."""
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS

def extract_text_from_pdf(filepath):
    """Extract text from a PDF file."""
    text = ""
    try:
        with open(filepath, "rb") as pdf_file:
            reader = PyPDF2.PdfReader(pdf_file)
            for page in reader.pages:
                text += page.extract_text()
    except Exception as e:
        logger.error(f"Error extracting text from PDF: {e}")
    return text

def extract_text_from_docx(filepath):
    """Extract text from a Word document."""
    text = ""
    try:
        doc = docx.Document(filepath)
        for paragraph in doc.paragraphs:
            text += paragraph.text + "\n"
    except Exception as e:
        logger.error(f"Error extracting text from DOCX: {e}")
    return text

@app.route("/upload", methods=["POST"])
def upload_file():
    """Upload a course booklet (PDF or Word)."""
    global uploaded_content

    if "file" not in request.files:
        return jsonify({"error": "No file part in the request"}), 400

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"error": "No file selected"}), 400

    if file:
        filename = secure_filename(file.filename)
        filepath = os.path.join(app.config["UPLOAD_FOLDER"], filename)
        file.save(filepath)
        logger.info(f"File uploaded: {filename}")

        # Extract text based on file type
        if filename.endswith(".pdf"):
            uploaded_content = extract_text_from_pdf(filepath)
        elif filename.endswith(".docx"):
            uploaded_content = extract_text_from_docx(filepath)
        else:
            uploaded_content = extract_text_from_pdf(filepath)

        if not uploaded_content.strip():
            return jsonify({"error": "Failed to extract text from the document"}), 500

        return jsonify({"message": "File uploaded and content extracted successfully"}), 200
    else:
        print(file.filename)
        return jsonify({"error": "Invalid file type"}), 400

@app.route("/generate", methods=["POST"])
def generate():
    """Generate a response using the uploaded course content."""
    global uploaded_content

    if not uploaded_content:

        return jsonify({"error": "No course content uploaded yet"}), 400
    
    try:
        # Parse and validate input
        data = request.get_json()
        if not data or "message" not in data:
            return jsonify({"error": "Invalid request, 'message' field is required"}), 400

        user_input = data["message"]
        logger.info(f"Received user input: {user_input}")

        # Use OpenAI with course content as context
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": f"Use the following course content to answer queries: {uploaded_content[:4000]}"},
                {"role": "user", "content": user_input}
            ]
        )

        # Extract and return the response
        generated_text = response.choices[0].message["content"]
        logger.info(f"Generated response: {generated_text}")
        return jsonify({"response": generated_text})

    except openai.OpenAIError as e:
        logger.error(f"OpenAI API error: {str(e)}")
        return jsonify({"error": "OpenAI API error, please try again later"}), 500

    except Exception as e:
        logger.error(f"Unexpected error: {str(e)}")
        return jsonify({"error": "An unexpected error occurred"}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
