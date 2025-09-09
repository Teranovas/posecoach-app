from typing import Dict, Any, List, Tuple, Optional
import mediapipe as mp
import cv2
import numpy as np
import math

mp_pose = mp.solutions.pose
PoseLandmark = mp_pose.PoseLandmark


# -----------------------
# 유틸 함수
# -----------------------
def _calculate_angle(a: Tuple[float, float], b: Tuple[float, float], c: Tuple[float, float]) -> float:
    """세 점(a, b, c) 사이의 각도(도)"""
    a = np.array(a, dtype=np.float64)
    b = np.array(b, dtype=np.float64)
    c = np.array(c, dtype=np.float64)
    ba = a - b
    bc = c - b
    denom = (np.linalg.norm(ba) * np.linalg.norm(bc))
    if denom == 0:
        return float("nan")
    cosine = np.dot(ba, bc) / denom
    cosine = float(np.clip(cosine, -1.0, 1.0))
    angle_rad = math.acos(cosine)
    return math.degrees(angle_rad)


def _torso_incline_deg(shoulder: Tuple[float, float], hip: Tuple[float, float]) -> float:
    """어깨→엉덩이 벡터와 수직축(위쪽) 사이의 각도(도). 0이면 곧게 선 상태."""
    vx = shoulder[0] - hip[0]
    vy = shoulder[1] - hip[1]
    v = np.array([vx, vy], dtype=np.float64)
    y_axis = np.array([0.0, -1.0], dtype=np.float64)
    denom = (np.linalg.norm(v) * np.linalg.norm(y_axis))
    if denom == 0:
        return float("nan")
    cosine = np.dot(v, y_axis) / denom
    cosine = float(np.clip(cosine, -1.0, 1.0))
    angle_rad = math.acos(cosine)
    return math.degrees(angle_rad)


def _landmarks_to_list(landmarks: List[Any]) -> List[Dict[str, float]]:
    out = []
    for lm in landmarks:
        out.append({
            "x": float(lm.x),
            "y": float(lm.y),
            "z": float(lm.z),
            "visibility": float(lm.visibility),
        })
    return out


def _px(pt, img_w: int, img_h: int) -> Tuple[float, float]:
    return (pt.x * img_w, pt.y * img_h)


# -----------------------
# 메인 분석 함수
# -----------------------
def analyze_pose_and_feedback(image_bgr: np.ndarray, mode: Optional[str] = None) -> Dict[str, Any]:
    """
    MediaPipe Pose로 랜드마크/각도/메트릭 계산 후 피드백 생성
    mode:
      - None: 일반 규칙(기본)
      - "squat": 스쿼트 전용 규칙 보강
      - "pushup": 푸시업 전용 규칙 보강
    """
    if image_bgr is None or image_bgr.size == 0:
        return {"ok": False, "message": "입력 이미지가 비어 있습니다."}

    img_h, img_w = image_bgr.shape[:2]
    image_rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)

    with mp_pose.Pose(
        static_image_mode=True,
        model_complexity=1,
        enable_segmentation=False,
        min_detection_confidence=0.5
    ) as pose:
        results = pose.process(image_rgb)

    if not results.pose_landmarks:
        return {"ok": False, "message": "사람의 자세를 감지하지 못했습니다."}

    lms = results.pose_landmarks.landmark
    lm_json = _landmarks_to_list(lms)

    def P(idx: PoseLandmark) -> Tuple[float, float]:
        return _px(lms[idx.value], img_w, img_h)

    def visible(idx: PoseLandmark, thr: float = 0.5) -> bool:
        return float(lms[idx.value].visibility) >= thr

    # 좌표 가져오기
    L_SH = P(PoseLandmark.LEFT_SHOULDER)
    R_SH = P(PoseLandmark.RIGHT_SHOULDER)
    L_EL = P(PoseLandmark.LEFT_ELBOW)
    R_EL = P(PoseLandmark.RIGHT_ELBOW)
    L_WR = P(PoseLandmark.LEFT_WRIST)
    R_WR = P(PoseLandmark.RIGHT_WRIST)
    L_HP = P(PoseLandmark.LEFT_HIP)
    R_HP = P(PoseLandmark.RIGHT_HIP)
    L_KN = P(PoseLandmark.LEFT_KNEE)
    R_KN = P(PoseLandmark.RIGHT_KNEE)
    L_AN = P(PoseLandmark.LEFT_ANKLE)
    R_AN = P(PoseLandmark.RIGHT_ANKLE)

    # 각도 계산
    angles: Dict[str, float] = {}

    def safe_angle(name: str, a_idx: PoseLandmark, b_idx: PoseLandmark, c_idx: PoseLandmark):
        if visible(a_idx) and visible(b_idx) and visible(c_idx):
            angles[name] = round(_calculate_angle(P(a_idx), P(b_idx), P(c_idx)), 2)

    safe_angle("left_elbow", PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
    safe_angle("right_elbow", PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
    safe_angle("left_shoulder", PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
    safe_angle("right_shoulder", PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
    safe_angle("left_hip", PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
    safe_angle("right_hip", PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
    safe_angle("left_knee", PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
    safe_angle("right_knee", PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

    # 메트릭 계산
    torso_incline_left = _torso_incline_deg(L_SH, L_HP) if visible(PoseLandmark.LEFT_SHOULDER) and visible(PoseLandmark.LEFT_HIP) else float("nan")
    torso_incline_right = _torso_incline_deg(R_SH, R_HP) if visible(PoseLandmark.RIGHT_SHOULDER) and visible(PoseLandmark.RIGHT_HIP) else float("nan")
    torso_incline = np.nanmean([torso_incline_left, torso_incline_right])

    shoulder_y_diff = abs(L_SH[1] - R_SH[1]) if (visible(PoseLandmark.LEFT_SHOULDER) and visible(PoseLandmark.RIGHT_SHOULDER)) else float("nan")

    # 간단한 Valgus(무릎 안짱) 판정
    valgus_tolerance = 0.06 * img_w
    left_valgus = (visible(PoseLandmark.LEFT_KNEE) and visible(PoseLandmark.LEFT_ANKLE)
                   and (L_AN[0] - L_KN[0]) < -valgus_tolerance)
    right_valgus = (visible(PoseLandmark.RIGHT_KNEE) and visible(PoseLandmark.RIGHT_ANKLE)
                    and (R_KN[0] - R_AN[0]) > valgus_tolerance)

    metrics = {
        "torso_incline_deg": round(float(torso_incline), 2) if not math.isnan(torso_incline) else None,
        "shoulder_height_diff_px": round(float(shoulder_y_diff), 2) if not math.isnan(shoulder_y_diff) else None,
        "left_knee_valgus": bool(left_valgus),
        "right_knee_valgus": bool(right_valgus),
        "image_size": {"width": int(img_w), "height": int(img_h)},
        "mode": mode or "default"
    }

    # -----------------------
    # 피드백 생성 (기본 + 모드별 규칙)
    # -----------------------
    feedback: List[str] = []

    # 기본(공통) 피드백
    def shoulder_msg(side: str):
        ang = angles.get(f"{side}_shoulder")
        if ang is None:
            return
        if ang < 30:
            feedback.append(f"{'왼' if side=='left' else '오'}팔을 더 들어 올려보세요 (어깨 각도 {ang}°).")
        elif 75 <= ang <= 105:
            feedback.append(f"{'왼' if side=='left' else '오'}팔 높이가 좋습니다 (약 {ang}°).")
        elif ang > 160:
            feedback.append(f"{'왼' if side=='left' else '오'}팔이 머리 위까지 올라갔어요 (약 {ang}°).")

    def elbow_msg(side: str):
        ang = angles.get(f"{side}_elbow")
        if ang is None:
            return
        if ang < 50:
            feedback.append(f"{'왼' if side=='left' else '오'}팔꿈치가 많이 접혔어요 (약 {ang}°).")
        elif ang > 160:
            feedback.append(f"{'왼' if side=='left' else '오'}팔꿈치를 조금 더 접어도 좋아요 (약 {ang}°).")

    def knee_msg(side: str):
        ang = angles.get(f"{side}_knee")
        if ang is None:
            return
        if ang > 160:
            feedback.append(f"{'왼' if side=='left' else '오'}무릎을 더 굽혀도 좋아요 (약 {ang}°).")
        elif 70 <= ang <= 120:
            feedback.append(f"{'왼' if side=='left' else '오'}무릎 각도 양호합니다 (약 {ang}°).")

    shoulder_msg("left"); shoulder_msg("right")
    elbow_msg("left"); elbow_msg("right")
    knee_msg("left"); knee_msg("right")

    if metrics["torso_incline_deg"] is not None:
        if metrics["torso_incline_deg"] > 30:
            feedback.append(f"상체가 많이 숙여졌어요 (기울기 약 {metrics['torso_incline_deg']}°).")
        elif metrics["torso_incline_deg"] < 10:
            feedback.append(f"상체가 안정적으로 곧게 서 있어요 (기울기 약 {metrics['torso_incline_deg']}°).")

    if metrics["shoulder_height_diff_px"] is not None:
        if metrics["shoulder_height_diff_px"] > 0.04 * img_h:
            feedback.append("좌우 어깨 높이가 다릅니다. 어깨를 수평으로 맞춰보세요.")

    if metrics["left_knee_valgus"]:
        feedback.append("왼쪽 무릎이 안쪽으로 말렸어요. 무릎과 발끝 방향을 일치시키세요.")
    if metrics["right_knee_valgus"]:
        feedback.append("오른쪽 무릎이 안쪽으로 말렸어요. 무릎과 발끝 방향을 일치시키세요.")

    # -----------------------
    # 모드별 보강 규칙
    # -----------------------
    def within(v: Optional[float], lo: float, hi: float) -> Optional[bool]:
        if v is None or math.isnan(v):
            return None
        return (lo <= v <= hi)

    if mode == "squat":
        # 스쿼트: 무릎 각도 70~120° 근처, 상체 기울기 10~35° 권장
        lk = angles.get("left_knee"); rk = angles.get("right_knee")
        ti = metrics["torso_incline_deg"]

        ok_knee = []
        for side, ang in (("왼", lk), ("오", rk)):
            if ang is not None:
                if within(ang, 70, 120):
                    ok_knee.append(side)
                elif ang > 140:
                    feedback.append(f"{side}무릎이 덜 굽혀졌어요 (약 {ang}°). 앉는 깊이를 더 주세요.")
                elif ang < 60:
                    feedback.append(f"{side}무릎이 지나치게 접혔어요 (약 {ang}°). 무릎에 무리가 갈 수 있어요.")
        if len(ok_knee) == 2:
            feedback.append("양쪽 무릎 각도가 스쿼트 범위에 잘 들어왔습니다.")

        if ti is not None:
            if ti < 8:
                feedback.append("상체가 너무 수직입니다. 엉덩이를 뒤로 빼며 약간 상체를 숙여보세요.")
            elif ti > 40:
                feedback.append("상체가 과도하게 숙여졌습니다. 가슴을 더 열어보세요.")

    elif mode == "pushup":
        # 푸시업: 몸통 일직선(엉덩이 각도>160°), 팔꿈치 80~110°(하강 구간 가정), 상체 기울기 과도X
        le = angles.get("left_elbow"); re = angles.get("right_elbow")
        lh = angles.get("left_hip"); rh = angles.get("right_hip")
        ti = metrics["torso_incline_deg"]

        # 팔꿈치
        ok_elbow = []
        for side, ang in (("왼", le), ("오", re)):
            if ang is not None:
                if within(ang, 80, 110):
                    ok_elbow.append(side)
                elif ang >= 150:
                    feedback.append(f"{side}팔꿈치가 거의 펴져 있어요 (약 {ang}°). 가슴을 더 낮춰보세요.")
                elif ang <= 60:
                    feedback.append(f"{side}팔꿈치가 너무 접혔어요 (약 {ang}°). 어깨에 과부하가 갈 수 있어요.")
        if len(ok_elbow) == 2:
            feedback.append("양쪽 팔꿈치 각도가 푸시업 범위에 적절합니다.")

        # 엉덩이(플랭크 라인)
        hip_ok_count = 0
        for side, ang in (("왼", lh), ("오", rh)):
            if ang is not None:
                if ang >= 160:
                    hip_ok_count += 1
                else:
                    feedback.append(f"{side}쪽 엉덩이가 꺼졌거나 들렸어요 (엉덩이 각도 {ang}°). 몸통을 일직선으로 유지하세요.")
        if hip_ok_count == 2:
            feedback.append("몸통이 전반적으로 일직선에 가깝습니다.")

        # 상체 기울기 (카메라 각도 영향 있지만 과도한 경우 보정)
        if ti is not None and ti > 45:
            feedback.append("상체 기울기가 커 보입니다. 팔꿈치 각도와 몸통 정렬을 다시 확인하세요.")

    # 피드백이 하나도 없다면 기본 메시지
    if not feedback:
        feedback.append("기본 자세가 전반적으로 안정적입니다.")

    return {
        "ok": True,
        "landmarks": lm_json,
        "angles": angles,
        "metrics": metrics,
        "feedback": feedback
    }
