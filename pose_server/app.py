from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route("/analyze_pose", methods=["POST"])
def analyze_pose():
    if 'image' not in request.files:
        return jsonify({"error": "No image provided"}), 400

    image = request.files['image']
    # 실제 분석 로직은 이후 analyzer 모듈로 분리 예정
    return jsonify({
        "pose": "squat",
        "feedback": "허리를 더 펴세요.",
        "score": 76
    })

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
