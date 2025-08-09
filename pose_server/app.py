from flask import Flask, request, jsonify
from flask_cors import CORS  
import cv2
import numpy as np
import mediapipe as mp
import tempfile
import os

app = Flask(__name__)
CORS(app)

mp_pose = mp.solutions.pose
pose = mp_pose.Pose(static_image_mode=True)


def calculate_angle(a, b, c):
    a = np.array([a.x, a.y])
    b = np.array([b.x, b.y])
    c = np.array([c.x, c.y])

    ba = a - b
    bc = c - b

    cosine_angle = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc))
    angle = np.arccos(np.clip(cosine_angle, -1.0, 1.0))
    return np.degrees(angle)


@app.route("/analyze_pose", methods=["POST"])
def analyze_pose():
    if 'image' not in request.files:
        return jsonify({"error": "No image provided"}), 400

    file = request.files['image']
    with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp_file:
        file.save(temp_file.name)
        image_path = temp_file.name

    image = cv2.imread(image_path)
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    results = pose.process(image_rgb)

    os.remove(image_path)

    if not results.pose_landmarks:
        return jsonify({
            "pose": "unknown",
            "feedback": "자세를 인식할 수 없습니다.",
            "score": 0
        })

    landmarks = results.pose_landmarks.landmark
    hip = landmarks[mp_pose.PoseLandmark.LEFT_HIP]
    knee = landmarks[mp_pose.PoseLandmark.LEFT_KNEE]
    ankle = landmarks[mp_pose.PoseLandmark.LEFT_ANKLE]

    angle = calculate_angle(hip, knee, ankle)
    feedback = "좋은 스쿼트입니다!" if 80 <= angle <= 100 else "무릎을 더 굽히세요."
    score = int(100 - abs(angle - 90))

    return jsonify({
        "pose": "squat",
        "feedback": feedback,
        "score": score
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001);