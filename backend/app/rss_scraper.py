import requests
import xml.etree.ElementTree as ET
import email.utils
from datetime import datetime
from sqlalchemy.orm import Session

from .database import SessionLocal
from .models import NewsArticle

FEEDS = [
    {"name": "Motorsport.com", "url": "https://www.motorsport.com/rss/f1/news/"},
    {"name": "Sky Sports F1", "url": "https://www.skysports.com/rss/12040"},
    {"name": "Autosport", "url": "https://www.autosport.com/rss/f1/news/"}
]

def parse_pub_date(date_str):
    try:
        # Converte le classiche date RSS (es. "Tue, 28 May 2026 15:30:00 GMT") in un oggetto Datetime
        return email.utils.parsedate_to_datetime(date_str)
    except Exception:
        return datetime.now()

def is_motorsport_related(title, link):
    keywords = [
        "f1", "formula 1", "formula one", "formula e", "f2", "formula 2", "f3", "formula 3",
        "wec", "endurance", "indy", "indycar", "motogp", "motorsport", "nascar",
        "verstappen", "hamilton", "leclerc", "ferrari", "mercedes", "red bull", "mclaren", 
        "aston martin", "fia", "prix", "sprint", "wrc", "rally"
    ]
    text_to_check = (title + " " + link).lower()
    return any(kw in text_to_check for kw in keywords)

def fetch_feed(feed_info, db: Session):
    print(f"Scaricando le notizie da {feed_info['name']}...")
    try:
        res = requests.get(feed_info['url'], timeout=10)
        res.raise_for_status()
        root = ET.fromstring(res.content)
        
        items = root.findall('.//item')
        saved_count = 0
        for item in items:
            if saved_count >= 15: # Ci fermiamo a 15 notizie valide per testata
                break
                
            title_node = item.find('title')
            link_node = item.find('link')
            pubdate_node = item.find('pubDate')
            
            title = title_node.text if title_node is not None else "No Title"
            link = link_node.text if link_node is not None else ""
            pub_date_str = pubdate_node.text if pubdate_node is not None else ""
            
            if not is_motorsport_related(title, link):
                continue # Scartiamo le notizie di altri sport (calcio, tennis, ecc)
                
            pub_date = parse_pub_date(pub_date_str)
            
            # Ricerca dell'immagine (Spesso salvata nel tag <enclosure>)
            image_url = None
            enclosure = item.find('enclosure')
            if enclosure is not None and enclosure.get('type', '').startswith('image'):
                image_url = enclosure.get('url')

            # Salvataggio nel Database se non è già presente!
            existing = db.query(NewsArticle).filter(NewsArticle.url == link).first()
            if not existing and link:
                new_article = NewsArticle(
                    title=title.strip(),
                    source=feed_info['name'],
                    url=link.strip(),
                    image_url=image_url,
                    published_at=pub_date
                )
                db.add(new_article)
                saved_count += 1
        
        db.commit()
        print(f"✅ Trovate nuove notizie per {feed_info['name']}")
    except Exception as e:
        print(f"❌ Errore scaricando da {feed_info['name']}: {e}")

def run_scraper():
    print("\n--- AVVIO SINCRONIZZAZIONE FEED RSS ---")
    db = SessionLocal()
    for feed in FEEDS:
        fetch_feed(feed, db)
    
    # Pulizia: Manteniamo solo le 100 notizie più recenti in assoluto nel DB!
    articles = db.query(NewsArticle).order_by(NewsArticle.published_at.desc()).all()
    if len(articles) > 100:
        db.query(NewsArticle).filter(NewsArticle.published_at < articles[99].published_at).delete()
        db.commit()

if __name__ == "__main__":
    run_scraper()