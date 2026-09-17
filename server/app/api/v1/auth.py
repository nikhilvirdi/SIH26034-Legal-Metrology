"""Authentication endpoints (stub).

Provides a minimal /token endpoint that returns a dummy bearer token so the
rest of the API can be exercised without a real auth service.
"""
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter(prefix="/auth", tags=["auth"])


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class LoginRequest(BaseModel):
    username: str
    password: str


@router.post("/token", response_model=TokenResponse, summary="Obtain access token (stub)")
async def login(body: LoginRequest) -> TokenResponse:
    """Returns a dummy token for any username/password combination.

    TODO: Replace with real JWT issuance once the auth service is ready.
    """
    return TokenResponse(access_token="DUMMY_TOKEN_REPLACE_ME")
