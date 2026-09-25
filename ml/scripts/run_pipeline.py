"""
RakshaCall Unified ML & Data Engineering Pipeline.
Single command to run the full lifecycle:
1. Dataset Verification & Inspection
2. Ingestion & Download
3. Schema Normalization & Train/Val/Test Split
4. Model Training (Multilingual Tactic Model + Conversation Stage Model)
5. Comprehensive Evaluation (Tactics, Stage, ASR, Vision)
6. Model Export & Backend Compatibility Verification

Usage:
  python ml/scripts/run_pipeline.py --stage all
  python ml/scripts/run_pipeline.py --stage dataset
  python ml/scripts/run_pipeline.py --stage train
  python ml/scripts/run_pipeline.py --stage evaluate
  python ml/scripts/run_pipeline.py --stage export
"""

import os
import sys
import numpy as np
import torch
import argparse
import logging

# Ensure project root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("RakshaCallPipeline")

def run_dataset_stage():
    logger.info("==========================================")
    logger.info("STAGE 1: DATASET INGESTION & NORMALIZATION")
    logger.info("==========================================")
    
    from ml.scripts.inspect_datasets import inspect_all
    inspect_all()

    from ml.scripts.download_datasets import download_all
    download_all()

    from ml.scripts.build_rakshacall_dataset import build_dataset
    build_dataset()

    from ml.scripts.normalize_datasets import normalize_and_split
    normalize_and_split()
    
    logger.info("Dataset stage completed successfully.")

def run_train_stage():
    logger.info("==========================================")
    logger.info("STAGE 2: MODEL TRAINING (MULTI-TASK & STAGE)")
    logger.info("==========================================")
    
    from ml.training.train_multilingual_tactic import train_model
    train_model(epochs=25, lr=0.005)

    from ml.training.train_conversation_stage import train_stage_model
    train_stage_model(epochs=25, lr=0.003)

    logger.info("Training stage completed successfully.")

def run_evaluate_stage():
    logger.info("==========================================")
    logger.info("STAGE 3: MULTIMODAL MODEL EVALUATION")
    logger.info("==========================================")

    from ml.evaluation.eval_tactics import evaluate_test_set
    evaluate_test_set()

    from ml.evaluation.eval_asr import evaluate_asr_providers
    evaluate_asr_providers()

    from ml.evaluation.eval_vision import evaluate_yolo11_context
    evaluate_yolo11_context()

    logger.info("Evaluation stage completed successfully.")

def run_export_stage():
    logger.info("==========================================")
    logger.info("STAGE 4: MODEL EXPORT & BACKEND CHECK")
    logger.info("==========================================")

    # Verify exported model artifacts exist and load
    from ml.models.scam_classifier.v1.inference import TrainedScamClassifier
    clf = TrainedScamClassifier()
    if clf.is_loaded:
        logger.info(f"Verified Model Artifacts: {clf.config.get('model_name')}")
        test_pred = clf.predict("Vanakkam, naan CBI officer pesuren. Ungal panatha transfer pannunga.")
        logger.info(f"Sample Prediction Test: Scam Prob={test_pred['scam_probability']} | Detected={test_pred['detected_tactics']}")
    else:
        logger.error("Failed to load exported model artifacts!")

    # Verify Stage Model artifacts
    stage_weights = os.path.join(os.path.dirname(os.path.dirname(__file__)), "models", "stage_model", "v1", "stage_model_weights.pt")
    if os.path.exists(stage_weights):
        logger.info(f"Verified Stage Model Artifacts at {stage_weights}")
    else:
        logger.error(f"Stage model weights missing at {stage_weights}")

    logger.info("Export & Backend Compatibility Check Complete.")

def main():
    parser = argparse.ArgumentParser(description="RakshaCall End-to-End ML Pipeline")
    parser.add_argument(
        "--stage",
        type=str,
        default="all",
        choices=["dataset", "train", "evaluate", "export", "all"],
        help="Pipeline stage to execute: dataset | train | evaluate | export | all"
    )
    args = parser.parse_args()

    stage = args.stage.lower()
    if stage == "all":
        run_dataset_stage()
        run_train_stage()
        run_evaluate_stage()
        run_export_stage()
    elif stage == "dataset":
        run_dataset_stage()
    elif stage == "train":
        run_train_stage()
    elif stage == "evaluate":
        run_evaluate_stage()
    elif stage == "export":
        run_export_stage()

    logger.info(f"RakshaCall ML Pipeline Execution Finished for stage: {stage}")

if __name__ == "__main__":
    main()
