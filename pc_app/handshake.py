from PyQt5.QtWidgets import QApplication 
import sys 
import os 
from gui.app import ServerApp

def resource_path(rel_path):
    base_path = getattr(sys,'_MEIPASS',os.path.abspath("."))
    return os.path.join(base_path, rel_path)


def main():
    app = QApplication(sys.argv)
    window = ServerApp()
    window.show()
    sys.exit(app.exec_())
   
    print('Server is running...')
 

if __name__ == "__main__":
    main()