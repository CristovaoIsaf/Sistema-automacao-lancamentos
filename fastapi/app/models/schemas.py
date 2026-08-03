from typing import Any, Dict, List, Optional

from pydantic import BaseModel


class OCRResponse(BaseModel):
    success: bool
    text: Optional[str] = None
    confidence: Optional[float] = None
    metadata: Optional[Dict[str, Any]] = None
    accounting_data: Optional[Dict[str, Any]] = None
    filename: Optional[str] = None
    processed_at: Optional[str] = None


class LancamentoSugerido(BaseModel):
    tipo_lancamento: str
    categoria_contabil: str
    conta_chave: Optional[str] = None
    conta_debito: Optional[str] = None
    conta_credito: Optional[str] = None
    valor: str
    descricao: str
    entidade: Optional[str] = None
    nif: Optional[str] = None
    data: Optional[str] = None
    moeda: str = "AOA"
    confianca: Optional[float] = None


class AccountingSuggestion(BaseModel):
    success: bool
    lancamento: Optional[LancamentoSugerido] = None
    contas: Optional[Dict[str, str]] = None
    confianca_ocr: Optional[float] = None
    confianca_ia: Optional[float] = None
    sugestao_automatica: Optional[bool] = None
    modelo: Optional[str] = None


class ErrorResponse(BaseModel):
    success: bool = False
    error: str
    details: Optional[str] = None