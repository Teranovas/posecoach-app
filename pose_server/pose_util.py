# pose_server/pose_utils.py
# MediaPipe로 포즈 랜드마크를 추출하고, 각도를 계산해 피드백을 반환하는 유틸리티 모듈

from typing import Dict, Any, List, Tuple
import mediapipe as mp
import cv2
import numpy as np
import math

mp_pose = mp.solutions.pose
PoseLandmark = mp_pose.PoseLandmark


def _calculate_angle(a: Tuple[float, float], b: Tuple[float, float], c: Tuple[float, float]) -> float:
    """
    세 점(a, b, c)의 각도(도) 반환. b가 꼭짓점.
    좌표는 (x, y) 픽셀 좌표 기준.
    """
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