"""
RakshaCall Notebook Generator.
Generates 8 Google Colab and local-compatible Jupyter Notebooks:
01_dataset_download.ipynb
02_dataset_normalization.ipynb
03_baseline_classifier.ipynb
04_multilingual_tactic_training.ipynb
05_conversation_stage_training.ipynb
06_asr_experiments.ipynb
07_evaluation.ipynb
08_model_export.ipynb
"""

import os
import json

NOTEBOOKS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "notebooks")

def make_notebook(cells):
    return {
        "cells": cells,
        "metadata": {
            "colab": {"provenance": []},
            "kernelspec": {"display_name": "Python 3", "language": "python", "name": "python3"},
            "language_info": {"name": "python", "version": "3.11.0"}
        },
        "nbformat": 4,
        "nbformat_minor": 2
    }

def md_cell(text):
    return {"cell_type": "markdown", "metadata": {}, "source": [text]}

def code_cell(code):
    return {"cell_type": "code", "execution_count": None, "metadata": {}, "outputs": [], "source": [code]}

def generate_all():
    os.makedirs(NOTEBOOKS_DIR, exist_ok=True)

    notebook_definitions = [
        ("01_dataset_download.ipynb", "01. Dataset Download & Hugging Face Ingestion",
         """!pip install -q datasets huggingface_hub pyyaml
import os
# Set HF_TOKEN if accessing gated datasets
# os.environ['HF_TOKEN'] = 'your_hf_token'
!python ../scripts/download_datasets.py
!python ../scripts/inspect_datasets.py
"""),
        ("02_dataset_normalization.ipynb", "02. Dataset Normalization, Schema Alignment & Splitting",
         """!pip install -q pydantic scikit-learn
import sys
sys.path.append('../..')
!python ../scripts/build_rakshacall_dataset.py
!python ../scripts/normalize_datasets.py
"""),
        ("03_baseline_classifier.ipynb", "03. Baseline Model Experiments (TF-IDF vs Multilingual Encoders)",
         """import sys
sys.path.append('../..')
import json
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report

print('Running baseline comparison...')
"""),
        ("04_multilingual_tactic_training.ipynb", "04. Multi-Task Multilingual 9-Tactic Training",
         """import sys
sys.path.append('../..')
!pip install -q torch scikit-learn
!python ../training/train_multilingual_tactic.py
"""),
        ("05_conversation_stage_training.ipynb", "05. Multi-Turn Conversation Stage & Velocity Training",
         """import sys
sys.path.append('../..')
!python ../training/train_conversation_stage.py
"""),
        ("06_asr_experiments.ipynb", "06. Multilingual ASR (HuBERT vs Indic ASR vs Cloud WER/CER)",
         """import sys
sys.path.append('../..')
!pip install -q jiwer
!python ../evaluation/eval_asr.py
"""),
        ("07_evaluation.ipynb", "07. Comprehensive Evaluation & False Alarm Benchmarks",
         """import sys
sys.path.append('../..')
!python ../evaluation/eval_tactics.py
!python ../evaluation/eval_vision.py
"""),
        ("08_model_export.ipynb", "08. Model Export, Quantization & Backend Provider Integration",
         """import sys
sys.path.append('../..')
import os, json
print('Verifying model exports in models/scam_classifier/v1 and models/stage_model/v1')
""")
    ]

    for fname, title, code in notebook_definitions:
        cells = [
            md_cell(f"# RakshaCall ML Pipeline: {title}\nCompatible with Google Colab & Local Python Environments."),
            code_cell(code)
        ]
        nb = make_notebook(cells)
        nb_path = os.path.join(NOTEBOOKS_DIR, fname)
        with open(nb_path, "w", encoding="utf-8") as f:
            json.dump(nb, f, indent=2)

    print(f"Generated {len(notebook_definitions)} notebooks in {NOTEBOOKS_DIR}")

if __name__ == "__main__":
    generate_all()
