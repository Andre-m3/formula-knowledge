import requests

class WeatherService:
    def __init__(self):
        self.base_url = "https://api.open-meteo.com/v1/forecast"

    def get_forecast(self, lat: float, lon: float):
        """
        Chiama l'API gratuita di Open-Meteo per ottenere le previsioni
        per le coordinate geografiche di un circuito.
        """
        url = "https://api.open-meteo.com/v1/forecast"
        params = {
            "latitude": lat,
            "longitude": lon,
            "current": "temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,uv_index,weather_code",
            "wind_speed_unit": "kmh",
            "timezone": "auto"
        }
        try:
            response = requests.get(url, params=params, timeout=3)
            response.raise_for_status()
            data = response.json()
            return self._parse_weather_data(data)
        except Exception as e:
            print(f"Errore API Meteo: {e}")
            return None

    def _parse_weather_data(self, data):
        current = data.get("current", {})
        code = current.get("weather_code", 0)
        
        mapping = {
            0: "Clear", 1: "Mainly Clear", 2: "Partly Cloudy", 3: "Overcast",
            45: "Fog", 48: "Depositing Rime Fog",
            51: "Light Drizzle", 53: "Moderate Drizzle", 55: "Dense Drizzle",
            61: "Slight Rain", 63: "Moderate Rain", 65: "Heavy Rain",
            71: "Slight Snowfall", 73: "Moderate Snowfall", 75: "Heavy Snowfall",
            95: "Thunderstorm"
        }
        
        return {
            "status": mapping.get(code, "Cloudy"),
            "temp": f"{int(round(current.get('temperature_2m', 0)))}°C",
            "humidity": f"{int(current.get('relative_humidity_2m', 0))}%",
            "feels_like": f"{int(round(current.get('apparent_temperature', 0)))}°C",
            "wind": f"{int(round(current.get('wind_speed_10m', 0)))} km/h",
            "uv": str(int(round(current.get("uv_index", 0))))
        }
