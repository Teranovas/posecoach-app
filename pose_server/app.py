from flask import Flask, request, jsonify
from flask_cors import CORS
import cv2
import numpy as np

# 파일명에 맞게 수정됨
from .pose_util import analyze_pose_and_feedback  

app = Flask(__name__)
CORS(app)

@app.route("/analyze_pose", methods=["POST"])
def analyze_pose():
    resp_format = request.args.get("format", "simple")  # simple | full
    mode = request.args.get("mode")

    if 'image' not in request.files:
        return jsonify({"ok": False, "message": "No image provided"}), 400

    file = request.files['image']
    file_bytes = np.frombuffer(file.read(), np.uint8)
    image = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)
    if image is None:
        return jsonify({"ok": False, "message": "Invalid image"}), 400

    result = analyze_pose_and_feedback(image_bgr=image)
    if not result.get("ok"):
        if resp_format == "simple":
            return jsonify({"pose": "unknown", "feedback": "자세를 인식할 수 없습니다.", "score": 0}), 200
        return jsonify(result), 400

    if resp_format == "full":
        return jsonify(result), 200

    # simple 포맷
    feedback_list = result.get("feedback", [])
    penalty = min(len([f for f in feedback_list if "좋습니다" not in f and "안정적" not in f]) * 5, 30)
    score = max(50, 80 - penalty)
    return jsonify({
        "pose": mode if mode else "squat",
        "feedback": feedback_list[0] if feedback_list else "기본 자세가 전반적으로 안정적입니다.",
        "score": score
    }), 200

@app.route("/ping", methods=["GET"])
def ping():
    return jsonify({"ok": True, "service": "PoseCoach-Python-Server", "port": 5001, "version": "v0.2"}), 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)
