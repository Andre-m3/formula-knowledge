import requests
import time
from datetime import datetime

class WeatherService:
    _cache = {}
    CACHE_TTL = 1800  # 30 minuti per il meteo

    def __init__(self):
        self.base_url = "https://api.open-meteo.com/v1/forecast"

    def get_forecast(self, lat: float, lon: float):
        cache_key = f"weather_{lat}_{lon}"
        if cache_key in self._cache:
            data, timestamp = self._cache[cache_key]
            if time.time() - timestamp < self.CACHE_TTL:
                return data

        params = {
            "latitude": lat,
            "longitude": lon,
            "current": "temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,uv_index,weather_code,precipitation_probability",
            "daily": "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max",
            "wind_speed_unit": "kmh",
            "timezone": "auto",
            "forecast_days": 5
        }
        try:
            response = requests.get(self.base_url, params=params, timeout=3)
            response.raise_for_status()
            data = response.json()
            parsed = self._parse_weather_data(data)
            self._cache[cache_key] = (parsed, time.time())
            return parsed
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
        
        daily_forecasts = []
        daily_data = data.get("daily", {})
        if daily_data.get("time"):
            for i in range(len(daily_data["time"])):
                day_date = datetime.strptime(daily_data["time"][i], "%Y-%m-%d")
                daily_forecasts.append({
                    "day": day_date.strftime("%A"),
                    "status": mapping.get(daily_data["weather_code"][i], "Cloudy"),
                    "temp_max": f"{int(round(daily_data['temperature_2m_max'][i]))}°",
                    "temp_min": f"{int(round(daily_data['temperature_2m_min'][i]))}°",
                    "wind": f"{int(round(daily_data['wind_speed_10m_max'][i]))} km/h",
                    "rain_probability": f"{daily_data['precipitation_probability_max'][i]}%"
                })

        return {
            "status": mapping.get(code, "Cloudy"),
            "temp": f"{int(round(current.get('temperature_2m', 0)))}°C",
            "humidity": f"{int(current.get('relative_humidity_2m', 0))}%",
            "feels_like": f"{int(round(current.get('apparent_temperature', 0)))}°C",
            "wind": f"{int(round(current.get('wind_speed_10m', 0)))} km/h",
            "uv": str(int(round(current.get("uv_index", 0)))),
            "rain_probability": f"{current.get('precipitation_probability', 0)}%",
            "daily": daily_forecasts
        }
