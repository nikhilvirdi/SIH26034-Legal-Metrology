from pathlib import Path

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    APP_NAME: str = "Legal Metrology Inspection Engine"
    DEBUG: bool = True
    PORT: int = 8000
    HOST: str = "0.0.0.0"
    DATABASE_URL: str = "sqlite:///./dummy.db"

    # Storage paths
    BASE_DIR: Path = Path(__file__).resolve().parent.parent
    STORAGE_DIR: Path = BASE_DIR / "storage"
    RAW_DIR: Path = STORAGE_DIR / "raw"
    CROPS_DIR: Path = STORAGE_DIR / "crops"
    ANNOTATED_DIR: Path = STORAGE_DIR / "annotated"
    WEIGHTS_DIR: Path = BASE_DIR / "weights"

    # Calibration
    KNOWN_ARUCO_SIZE_MM: float = 40.0
    ARUCO_DICT_NAME: str = "DICT_6X6_100"

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()

# Ensure runtime directories exist
settings.RAW_DIR.mkdir(parents=True, exist_ok=True)
settings.CROPS_DIR.mkdir(parents=True, exist_ok=True)
settings.ANNOTATED_DIR.mkdir(parents=True, exist_ok=True)
