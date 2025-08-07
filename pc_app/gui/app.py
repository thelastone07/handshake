import threading
from PyQt5.QtWidgets import (
    QWidget, QPushButton, QVBoxLayout, QLabel, QHBoxLayout,
    QDialog, QDialogButtonBox, QApplication, QFrame, QGraphicsDropShadowEffect
)
from PyQt5.QtGui import QPixmap, QFont, QIcon, QColor, QPalette, QLinearGradient, QPainter, QPen
from PyQt5.QtCore import QSize
from PyQt5.QtCore import Qt, QPropertyAnimation, QEasingCurve, QTimer
from crypto.qr import generate_qr
from network import listen

import os, sys 

def resource_path(rel_path):
    base_path = getattr(sys,'_MEIPASS',os.path.abspath("."))
    return os.path.join(base_path, rel_path)


class GradientLabel(QLabel):
    def __init__(self, text):
        super().__init__(text)
        self.setAlignment(Qt.AlignCenter)
        self.setFont(QFont("Segoe UI", 28, QFont.Bold))
        self.setText("")  # Clear text to avoid double rendering
        
    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.Antialiasing)
        
        # Create gradient
        gradient = QLinearGradient(0, 0, self.width(), 0)
        gradient.setColorAt(0, QColor("#EF4444"))  # Red
        gradient.setColorAt(0.5, QColor("#F97316"))  # Orange
        gradient.setColorAt(1, QColor("#EAB308"))  # Yellow
        
        # Set gradient as pen color
        painter.setPen(QPen(gradient, 1))
        painter.setFont(self.font())
        
        # Draw text with gradient
        painter.drawText(self.rect(), Qt.AlignCenter, "HANDSHAKE")


class ModernButton(QPushButton):
    def __init__(self, text, primary=False, size=None, icon=None):
        super().__init__(text)
        
        if size:
            self.setFixedSize(*size)
        
        if icon:
            self.setIcon(icon)
            self.setIconSize(QSize(20, 20))
        
        # Modern font
        font_size = 14 if primary else 11
        self.setFont(QFont("Segoe UI", font_size, QFont.Medium))
        
        # Color scheme
        if primary:
            bg_color = "#6366F1"  # Indigo
            hover_color = "#4F46E5"
            disabled_color = "#374151"
        else:
            bg_color = "#374151"  # Gray
            hover_color = "#4B5563"
            disabled_color = "#1F2937"
        
        self.setStyleSheet(f"""
            QPushButton {{
                background-color: {bg_color};
                color: white;
                border: none;
                border-radius: 12px;
                padding: 12px 24px;
                font-weight: 500;
            }}
            QPushButton:hover {{
                background-color: {hover_color};
                
            }}
            QPushButton:pressed {{
                background-color: {disabled_color};
                
            }}
            QPushButton:disabled {{
                background-color: {disabled_color};
                color: #9CA3AF;
            }}
        """)
        
        # Add shadow effect
        shadow = QGraphicsDropShadowEffect()
        shadow.setBlurRadius(20)
        shadow.setColor(QColor(0, 0, 0, 50))
        shadow.setOffset(0, 4)
        self.setGraphicsEffect(shadow)


class QRDialog(QDialog):
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("")
        self.setFixedSize(400, 450)  # Smaller dialog to fit QR image better
        self.setWindowFlags(
            Qt.Dialog | 
            Qt.FramelessWindowHint |
            Qt.WindowStaysOnTopHint
        )
        
        # Dark theme styling
        self.setStyleSheet("""
            QDialog {
                background-color: #111827;
                color: white;
                border-radius: 16px;
            }
        """)

        self.qr_label = QLabel(self)
        self.qr_label.setAlignment(Qt.AlignCenter)
        self.qr_label.setStyleSheet("""
            QLabel {
                background-color: #FFFFFF;
                border-radius: 20px;
                padding: 30px;
                margin: 20px;
            }
        """)
        
        pixmap = QPixmap(resource_path("assets/qr_code.png"))
        if not pixmap.isNull():
            self.qr_label.setPixmap(pixmap.scaled(200, 200, Qt.KeepAspectRatio, Qt.SmoothTransformation))
        else:
            self.qr_label.setText("QR code not found.")
            self.qr_label.setStyleSheet("""
                QLabel {
                    background-color: #1F2937;
                    border-radius: 20px;
                    padding: 30px;
                    margin: 20px;
                    color: #9CA3AF;
                }
            """)

        # Close button below QR
        close_btn = ModernButton("Close", size=(100, 40))
        close_btn.clicked.connect(self.reject)

        # Layout
        layout = QVBoxLayout()
        layout.addWidget(self.qr_label, alignment=Qt.AlignCenter)
        layout.addWidget(close_btn, alignment=Qt.AlignCenter)
        layout.setContentsMargins(20, 20, 20, 20)  
        self.setLayout(layout)


class ServerApp(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Handshake") 
        self.setFixedSize(400, 500)
        self.setWindowFlags(
            Qt.Window |
            Qt.WindowMinimizeButtonHint |
            Qt.WindowCloseButtonHint
        )
        self.setWindowIcon(QIcon("assets/icon.png"))
        
        # Dark theme styling
        self.setStyleSheet("""
            QWidget {
                background-color: #111827;
                color: white;
            }
        """)

        # Header with gradient effect
        header_label = GradientLabel("HANDSHAKE")

        # Main action button
        self.start_button = ModernButton("Start Listening", primary=True, size=(220, 60))
        self.start_button.setEnabled(True)

        # QR button with icon (bottom left)
        qr_icon = QIcon(resource_path("assets/qr_icon.svg"))
        self.qr_button = ModernButton("", size=(50, 50), icon=qr_icon)
        self.qr_button.setEnabled(False)
        self.qr_button.setToolTip("Show QR Code")

        # Main layout
        main_layout = QVBoxLayout()
        main_layout.addWidget(header_label)
        main_layout.addStretch(1)
        main_layout.addWidget(self.start_button, alignment=Qt.AlignCenter)
        main_layout.addStretch(2)
        
        # Bottom layout for QR button
        bottom_layout = QHBoxLayout()
        bottom_layout.addWidget(self.qr_button, alignment=Qt.AlignLeft)
        bottom_layout.addStretch(1)
        
        main_layout.addLayout(bottom_layout)
        main_layout.addSpacing(20)
        
        self.setLayout(main_layout)

        # Connect events
        self.start_button.clicked.connect(self.start_listening)
        self.qr_button.clicked.connect(self.show_qr)

        self.listener_thread = None
        self.is_listening = False

    def start_listening(self):
        self.listener_thread = threading.Thread(target=listen.listen, args=(), daemon=True)
        self.listener_thread.start()

        generate_qr()
        self.qr_button.setEnabled(True)
        self.start_button.setEnabled(False)
        self.is_listening = True

    def show_qr(self):
        qr_dialog = QRDialog(self)
        qr_dialog.exec_()

