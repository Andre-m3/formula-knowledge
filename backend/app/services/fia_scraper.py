import fitz  # PyMuPDF
import requests
from bs4 import BeautifulSoup
import io
import json
import google.generativeai as genai
from .calendar_service import CalendarService

class FiaScraperService:
    def __init__(self):
        self.docs_url = "https://www.fia.com/documents/championships/fia-formula-one-world-championship-14/season/season-2026-2072"
        self.base_url = "https://www.fia.com"
        # Configurazione Gemini
        genai.configure(api_key="AIzaSyCHwvpD6Z5d4QQjUoNutmDx0jh6kz1EJ9k")
        self.model = genai.GenerativeModel('gemini-1.5-flash')

    def find_latest_submissions_pdf(self, gp_name: str) -> str:
        try:
            response = requests.get(self.docs_url, timeout=10)
            if response.status_code != 200:
                return None
            
            soup = BeautifulSoup(response.content, 'html.parser')
            links = soup.find_all('a', href=True)
            target_str = f"{gp_name} Grand Prix - Car Presentation Submissions".lower()
            
            for link in links:
                link_text = link.get_text().lower()
                if target_str in link_text and link['href'].endswith('.pdf'):
                    pdf_path = link['href']
                    if not pdf_path.startswith('http'):
                        return self.base_url + pdf_path
                    return pdf_path
            return None
        except Exception as e:
            print(f"Errore durante lo scraping FIA: {e}")
            return None

    def extract_text_from_pdf(self, pdf_url: str) -> str:
        response = requests.get(pdf_url)
        if response.status_code != 200:
            return ""
        
        pdf_stream = io.BytesIO(response.content)
        document = fitz.open(stream=pdf_stream, filetype="pdf")
        
        full_text = ""
        for page_num in range(len(document)):
            page = document.load_page(page_num)
            full_text += page.get_text("text")
        return full_text

    def parse_with_ai(self, raw_text: str):
        prompt = f"""
        Sei un esperto ingegnere aerodinamico di Formula 1. 
        Analizza il seguente testo estratto da un documento ufficiale FIA "Car Presentation Submissions".
        Estrai tutti gli aggiornamenti tecnici portati da ogni team.
        
        Regole:
        1. Restituisci i risultati in formato JSON puro.
        2. Per ogni team, crea un oggetto con 'team_name' e 'updates' (una lista di stringhe).
        3. Traduci gli aggiornamenti in ITALIANO tecnico ma comprensibile.
        4. Sii preciso sui componenti (es. 'Ala anteriore', 'Pance', 'Fondo').
        5. Se un team non ha aggiornamenti ("No changes"), non includerlo.

        Testo da analizzare:
        {raw_text}
        """
        
        try:
            response = self.model.generate_content(prompt)
            # Pulizia per estrarre solo il JSON se l'AI aggiunge markdown
            json_text = response.text.replace("```json", "").replace("```", "").strip()
            return json.loads(json_text)
        except Exception as e:
            print(f"Errore Gemini AI: {e}")
            return []

    def process_latest_car_presentation(self):
        calendar = CalendarService()
        current_race = calendar.get_current_or_next_race()
        gp_name = current_race["name"].replace(" Grand Prix", "")
        
        pdf_url = self.find_latest_submissions_pdf(gp_name)
        if not pdf_url:
            return {"status": "not_ready", "gp": gp_name}
        
        raw_text = self.extract_text_from_pdf(pdf_url)
        if not raw_text:
            return {"status": "not_ready", "gp": gp_name}
            
        ai_results = self.parse_with_ai(raw_text)
        
        return {
            "status": "ready",
            "gp": gp_name,
            "data": ai_results
        }
