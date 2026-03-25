import requests

class WeatherService:
    def __init__(self):
        self.base_url = "https://api.open-meteo.com/v1/forecast"

    def get_forecast(self, lat: float, lon: float):
        params = {
            "latitude": lat,
            "longitude": lon,
            "current": ["temperature_2m", "weather_code", "relative_humidity_2m", "apparent_temperature", "wind_speed_10m", "uv_index"],
            "timezone": "auto",
            "forecast_days": 1
        }
        
        try:
            response = requests.get(self.base_url, params=params)
            response.raise_for_status()
            data = response.json()
            return self._parse_weather_data(data)
        except Exception as e:
            print(f"Errore meteo: {e}")
            return {"status": "N/A", "temp": "--°C", "humidity": "--%", "feels_like": "--°C", "wind": "-- km/h", "uv": "0"}

    def _parse_weather_data(self, data):
        current = data["current"]
        code = current["weather_code"]
        
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
            "temp": f"{round(current['temperature_2m'])}°C",
            "humidity": f"{current['relative_humidity_2m']}%",
            "feels_like": f"{round(current['apparent_temperature'])}°C",
            "wind": f"{current['wind_speed_10m']} km/h",
            "uv": str(current["uv_index"])
        }
