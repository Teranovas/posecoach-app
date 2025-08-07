# PoseCoach 🧘
AI 자세 분석 헬스 트레이너 앱

## 구성
- Android 앱 (Kotlin + CameraX)
- Python 서버 (Flask + MediaPipe)
- 실시간 자세 분석 & 피드백 제공

## 폴더 구조
- `android_app/`: 사용자 앱
- `pose_server/`: AI 자세 분석 서버

## 기능 요약
- 자세 인식 (스쿼트, 런지, 팔굽혀펴기)
- 실시간 피드백 (문장 or 음성)
- 운동 기록 저장
- 올바른 자세 비교

## 기술 스택
| 파트        | 기술 |
|-------------|------|
| Android 앱  | Kotlin, Retrofit, CameraX |
| 서버        | Python, Flask or FastAPI |
| 분석        | MediaPipe, OpenCV, NumPy |
| 통신        | REST API or WebSocket |
| 배포        | Render, Railway (예정) |

## 📬 API 예시
`POST /analyze_pose`

Request: `image.jpg`  
Response:
```json
{
  "pose": "squat",
  "feedback": "허리를 더 펴세요.",
  "score": 76
}
