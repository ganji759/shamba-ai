"""The deployable model: preprocessing and softmax live inside the graph.

Apps send one RGB image as float32 [1, 3, H, W] with values in [0, 1].
The program returns class probabilities [1, num_classes]. Normalization is part
of the exported program, so Kotlin, Swift, JavaScript and Python clients cannot
drift apart on mean/std values.
"""

from __future__ import annotations

import timm
import torch
from torch import nn

IMAGENET_MEAN = (0.485, 0.456, 0.406)
IMAGENET_STD = (0.229, 0.224, 0.225)


def create_backbone(name: str, num_classes: int, pretrained: bool = True) -> nn.Module:
    return timm.create_model(name, pretrained=pretrained, num_classes=num_classes)


class DeployableClassifier(nn.Module):
    def __init__(self, backbone: nn.Module, mean=IMAGENET_MEAN, std=IMAGENET_STD):
        super().__init__()
        self.backbone = backbone
        self.register_buffer("mean", torch.tensor(mean).view(1, 3, 1, 1))
        self.register_buffer("std", torch.tensor(std).view(1, 3, 1, 1))

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x = (x - self.mean) / self.std
        return torch.softmax(self.backbone(x), dim=-1)


def load_checkpoint(path: str, map_location: str = "cpu") -> tuple[nn.Module, dict]:
    """Return (backbone with trained weights, checkpoint metadata)."""
    ckpt = torch.load(path, map_location=map_location, weights_only=False)
    backbone = create_backbone(ckpt["backbone"], len(ckpt["classes"]), pretrained=False)
    backbone.load_state_dict(ckpt["state_dict"])
    return backbone.eval(), ckpt
