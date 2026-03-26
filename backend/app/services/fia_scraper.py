import fitz  # PyMuPDF
import requests
from bs4 import BeautifulSoup
import io

class FiaScraperService:
    def __init__(self):
        self.base_url = "https://www.fia.com"

    def extract_text_from_pdf_url(self, pdf_url: str) -> str:
        """Scarica il PDF al volo e ne estrae il testo usando PyMuPDF."""
        response = requests.get(pdf_url)
        if response.status_code != 200:
            raise Exception("Impossibile scaricare il PDF")
        
        # Legge il PDF direttamente dalla memoria senza salvarlo sul disco
        pdf_stream = io.BytesIO(response.content)
        document = fitz.open(stream=pdf_stream, filetype="pdf")
        
        full_text = ""
        for page_num in range(len(document)):
            page = document.load_page(page_num)
            full_text += page.get_text("text")
            
        return full_text

    def parse_updates_from_text(self, raw_text: str):
        """
        QUI INTERVERRÀ L'AI. 
        Invieremo il raw_text a un LLM (es. OpenAI) chiedendogli di estrarre e tradurre.
        Per ora, simuliamo il risultato strutturato che l'AI ci restituirà, 
        esattamente formattato per la UI che hai in mente.
        """
        # SIMULAZIONE RISPOSTA AI
        return [
            {
                "team_name": "Scuderia Ferrari HP",
                "updates": [
                    "Modifica all'ala anteriore per aumentare il carico aerodinamico nelle curve lente.",
                    "Nuovo disegno delle pance laterali per migliorare il raffreddamento."
                ]
            },
            {
                "team_name": "McLaren Mastercard F1 Team",
                "updates": [
                    "Fondo vettura completamente ridisegnato per ridurre il porpoising."
                ]
            }
            # Nota: Red Bull e Mercedes non sono in lista perché non hanno portato aggiornamenti!
        ]

    def process_latest_car_presentation(self):
        """Funzione principale che il nostro backend chiamerà ogni venerdì."""
        # 1. Trovare l'URL del PDF (Simulato per ora)
        pdf_url = "https://www.fia.com/sites/default/files/decision-document/2024%20Miami%20Grand%20Prix%20-%20Car%20Presentation%20Submissions.pdf"
        # 2. Estrarre il testo
        # raw_text = self.extract_text_from_pdf_url(pdf_url)
        # 3. Parsare e strutturare (Simulato)
        return self.parse_updates_from_text("testo simulato")