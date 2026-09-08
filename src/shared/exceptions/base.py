class DomainError(Exception):
    code = "DOMAIN_ERROR"
    status_code = 500
    def __init__(self, message: str, *, code: str | None = None):
        super().__init__(message)
        self.message = message
        if code:
            self.code = code
class NotFoundError(DomainError):
    status_code = 404
class ValidationError(DomainError):
    status_code = 400
class ConflictError(DomainError):
    status_code = 409
class UnauthorizedError(DomainError):
    status_code = 401
class ExternalSystemError(DomainError):
    status_code = 502
class InfrastructureError(DomainError):
    status_code = 500

