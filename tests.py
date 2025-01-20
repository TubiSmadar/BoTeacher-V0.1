import os
import pytest
from flask import json
from app import app, UPLOAD_FOLDER, courses, student_chats


@pytest.fixture
def client():
    """Fixture to create a test client for the Flask app."""
    with app.test_client() as client:
        yield client


@pytest.fixture(autouse=True)
def setup_and_teardown():
    """Setup and teardown for each test."""
    os.makedirs(UPLOAD_FOLDER, exist_ok=True)
    courses.clear()
    student_chats.clear()
    yield
    for file in os.listdir(UPLOAD_FOLDER):
        os.remove(os.path.join(UPLOAD_FOLDER, file))


# Helper to create a sample course
def upload_sample_course(client, course_name="Test Course", file_path="test_data/sample.txt"):
    """Helper function to upload a sample course."""
    with open(file_path, "w") as f:
        f.write("Sample course content.")

    data = {"course_name": course_name}
    with open(file_path, "rb") as file:
        data["file"] = (file, "sample.txt")
        response = client.post("/lecturer/upload_course", data=data, content_type="multipart/form-data")
    os.remove(file_path)
    return response


def test_upload_course_valid(client):
    """Test uploading a valid course."""
    response = upload_sample_course(client)
    assert response.status_code == 200
    assert "course_id" in response.get_json()


def test_upload_course_missing_file(client):
    """Test uploading a course without a file."""
    data = {"course_name": "Test Course"}
    response = client.post("/lecturer/upload_course", data=data, content_type="multipart/form-data")
    assert response.status_code == 400
    assert response.get_json()["error"] == "File and course_name are required"


def test_upload_course_invalid_file(client):
    """Test uploading a course with an invalid file type."""
    invalid_file_path = "test_data/invalid_file.exe"
    with open(invalid_file_path, "wb") as f:
        f.write(b"Invalid file content")
    data = {"course_name": "Test Course"}
    with open(invalid_file_path, "rb") as invalid_file:
        data["file"] = (invalid_file, "invalid_file.exe")
        response = client.post("/lecturer/upload_course", data=data, content_type="multipart/form-data")
    os.remove(invalid_file_path)
    assert response.status_code == 400
    assert response.get_json()["error"] == "Invalid file type"


def test_student_chat_valid(client):
    """Test a valid student chat interaction."""
    # Upload a course
    course_response = upload_sample_course(client)
    course_id = course_response.get_json()["course_id"]

    payload = {
        "student_id": "student1",
        "course_id": course_id,
        "message": "What is the content of this course?",
    }
    response = client.post("/student/chat", json=payload)
    assert response.status_code == 200
    assert "response" in response.get_json()


def test_student_chat_no_course(client):
    """Test chatting with a non-existent course."""
    payload = {
        "student_id": "student1",
        "course_id": "invalid_course_id",
        "message": "Hello",
    }
    response = client.post("/student/chat", json=payload)
    assert response.status_code == 404
    assert response.get_json()["error"] == "Invalid course_id"


def test_get_chat_history_valid(client):
    """Test retrieving valid chat history."""
    # Upload a course
    course_response = upload_sample_course(client)
    course_id = course_response.get_json()["course_id"]

    # Start a chat
    payload = {
        "student_id": "student1",
        "course_id": course_id,
        "message": "Hello, what is this course about?",
    }
    client.post("/student/chat", json=payload)

    # Retrieve chat history
    history_payload = {"student_id": "student1", "course_id": course_id}
    response = client.post("/student/get_chat", json=history_payload)
    assert response.status_code == 200
    assert len(response.get_json()["chat_history"]) == 2


def test_get_chat_history_no_chat(client):
    """Test retrieving chat history for a non-existent chat."""
    payload = {"student_id": "student1", "course_id": "non_existent_course"}
    response = client.post("/student/get_chat", json=payload)
    assert response.status_code == 404
    assert response.get_json()["error"] == "No chat history found for this course"


def test_get_courses_valid(client):
    """Test retrieving all available courses."""
    upload_sample_course(client, course_name="Course 1")
    upload_sample_course(client, course_name="Course 2")

    response = client.get("/student/courses")
    assert response.status_code == 200
    assert len(response.get_json()) == 2


def test_get_courses_empty(client):
    """Test retrieving courses when none are uploaded."""
    response = client.get("/student/courses")
    assert response.status_code == 404
    assert response.get_json()["error"] == "No courses available"


def test_upload_empty_content_course(client):
    """Test uploading a file with no extractable content."""
    empty_file_path = "test_data/empty.txt"
    with open(empty_file_path, "w") as f:
        f.write("")

    data = {"course_name": "Empty Course"}
    with open(empty_file_path, "rb") as file:
        data["file"] = (file, "empty.txt")
        response = client.post("/lecturer/upload_course", data=data, content_type="multipart/form-data")
    os.remove(empty_file_path)
    assert response.status_code == 500
    assert response.get_json()["error"] == "Failed to extract text from the document"
