"""Deixa os modulos do gateway importaveis sem instalar pacote.

Os modulos se importam entre si por nome curto (`import clock`, `from state import
State`) porque em producao rodam como script/executavel a partir da propria
pasta. Os testes replicam esse sys.path.
"""
import os
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if RAIZ not in sys.path:
    sys.path.insert(0, RAIZ)
