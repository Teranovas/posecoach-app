
# PoseCoach

**PoseCoach**는 AI 기반 피트니스 트레이너 앱으로, 사용자의 운동 자세를 실시간으로 분석하고 피드백을 제공합니다.  
Python 서버(MediaPipe + OpenCV)와 Android 앱(CameraX + Retrofit + MVVM)을 결합하여 **정확한 자세 분석**, **즉각적인 피드백**, **UX 친화적인 UI**를 구현합니다.

---

## 주요 기능

- **이미지/카메라 업로드 → 서버 분석**
- **MediaPipe Pose 기반 33개 관절 추출 + 각도/메트릭 계산**
- **운동 모드별(squat, pushup 등) 피드백 생성**
- **Simple/Full 모드 결과 제공 (점수/자세 데이터/피드백 리스트)**
- **오버레이 PNG 이미지로 분석 시각화**
- **TTS 음성 안내 (선택된 피드백 자동 읽기)**
- **Android 앱 UI: 갤러리 + CameraX 업로드, RecyclerView 카드 피드백**

---

## 핵심 기술 (Technical Highlights)

- **Pose Estimation**  
  MediaPipe Pose를 활용하여 33개 관절 좌표 추출,  
  어깨/팔꿈치/엉덩이/무릎 각도 + 상체 기울기 + 어깨 높이 차 + 무릎 발란구스 계산

- **Flask 서버 (Python)**  
  이미지 업로드 → NumPy/ OpenCV 변환 → Pose 분석 후 JSON/PNG 응답  
  `format=simple` : 점수 + 첫 피드백  
  `format=full` : 모든 landmarks/angles/metrics + 피드백 리스트

- **안드로이드 앱 (Kotlin)**  
  - **MVVM 아키텍처**: Repository → ViewModel → UI  
  - **Retrofit + OkHttp**: 서버 연동  
  - **CameraX**: 실시간 촬영 + 업로드  
  - **RecyclerView**: 피드백 카드 UI  
  - **TTS(TextToSpeech)**: 첫 번째 피드백 음성 안내

- **Git 브랜치 전략**  
  - `feature/python-server`: 서버 개발  
  - `feature/exercise-modes`: 운동 모드별 로직 추가  
  - `feature/android-app`: 앱 개발  
  - 모든 기능은 안정화 후 `main`에 병합

---

## 개발 히스토리 요약

1. Flask + MediaPipe 기반 Pose 서버(v0.1) 구축
2. 이미지 업로드 → JSON 응답 (landmarks + angles + feedback)
3. v0.2: Simple/Full 응답 분리 + 점수화 로직 추가
4. Android 앱: Retrofit 연결 + 갤러리 업로드 기능
5. CameraX 촬영 → 서버 업로드 → 오버레이 PNG 표시
6. RecyclerView + Adapter로 피드백 카드 UI 구현
7. TTS(TextToSpeech) 기능 추가 (피드백 음성 안내)

---

## 프로젝트 구조

```bash
posecoach/
├── pose_server/
│   ├── app.py
│   └── pose_util.py
├── app/
│   ├── ui/
│   ├── network/
│   └── repository/
├── assets/
└── README.md
```
---

## 실행방법

### 1) 사전 준비
  - Python 3.10+ (권장), pip 최신 버전
  - Android Studio 최신 버전, Android SDK 35
  - 같은 네트워크(PC ↔ 에뮬레이터/실기기) 연결


### 2) Python 서버 실행
  - 가상환경 & 패키치 설치

  ```bash
  cd posecoach/pose_server
  python -m venv .venv
  source .venv/bin/activate   # Windows: .venv\Scripts\activate
  pip install --upgrade pip wheel
  pip install -r requirements.txt  # 없으면 개별 설치: flask flask-cors opencv-python mediapipe numpy
  ```

  - 서버 실행
  ```bash
  python app.py
  서버 기본 포트: 5001
  ```

### 3) Android 앱 실행
  
  - 서버 주소 설정 app/build.gradle.kts
  ```bash
  defaultConfig {
    buildConfigField("String", "POSE_SERVER_BASE_URL", "\"http://10.0.2.2:5001/\"") // 에뮬레이터
  }
  ```

  - 실행
  앱 내 버튼 동작
    - Pick → 갤러리 이미지 업로드
    - Camera → 촬영 → 업로드
    - Simple → 점수 + 피드백
    - Full → 모든 지표 + 피드백 리스트
    - Overlay → 오버레이 PNG
    - TTS 스위치 → 피드백 음성 안내


## 실행결과

