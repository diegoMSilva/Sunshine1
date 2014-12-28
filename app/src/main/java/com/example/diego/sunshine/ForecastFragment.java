package com.example.diego.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    //create an adapter to setup the content of the string array into the listview properly
    static ArrayAdapter<String> arrayAdapter;


    public ForecastFragment() {
    }


    /*
     * Executes action of the life cycle of the fragment
      */
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Activate the menu handling for this fragment
        setHasOptionsMenu(true);
    }


    /*
    * Inflate the options menu
    */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }


    /*
    * Handles the on tap actions of the menu options
    *
    * If the user tap the refresh button create a new FetchWeatherTask object (an AsyncTask) that
    * is going to run on a background thread, the created object send a HTTPRequest to the
    * openweathermap.org and get a query result from their api, it processes the json string into a
    * json object and get fetch the needed information on its doInBackground method, and then return
    * the results to the main thread on its onPostExecute method
    */
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle action bar item clicks here
        //get the item id
        int id = item.getItemId();
        //return true if the id is the action refresh id
        if(id == R.id.action_refresh){
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("Recife");

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /*
    * Create a view object and initialize it with dummy data
    */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){

        //fill a string with the content of the listview
        String[] list_items = {
                "Today, min 10 - max 20, sunny",
                "Tomorrow, min 10 - max 20, sunny",
                "Monday min 10 - max 20, sunny",
                "Tuesday min 10 - max 20, sunny",
                "Wednesday min 10 - max 20, rainy",
                "Thursday min 10 - max 20, sunny",
                "Friday min 10 - max 20, cloudy",
        };

        //convert the array into an ArrayList object
        List forecast_info = new ArrayList<String>(Arrays.asList(list_items));

        arrayAdapter = new ArrayAdapter<String>(
                //activities of this fragment's parents
                getActivity(),
                //id of the layout where the textview object of each row is going to be placed
                R.layout.list_item_forecast,
                //id of the textview that is going to be used to generate all the textview obejcts
                R.id.list_item_forecast_textview,
                //the array data that is going to populate the textviews and consequently the layout
                forecast_info);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //connecting/binding the adapter to the listview
        //* find a reference to the listview traversing the tree of views from the root view node
        // note that we used the R.id to get the listview id
        ListView listView = (ListView) rootView.findViewById(R.id.list_view_forecast);

        //set the adapter to the listview
        listView.setAdapter(arrayAdapter);

        return rootView;
    }




    /*
    * Class FecthWeatherTask
    *
    * Get information from the openweather API on the cloud  and place into a raw json file
    */
    public class FetchWeatherTask extends AsyncTask<String,Void,String[]>{

        //Setup the log tag dynamically and synchronized with the class name
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            //making an HTTPRequest
            //read response from an input stream
            //cleaning the input stream

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            //set up the format in which the data must come in from the HTTPRequest
            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {
                //constructs the url for the openweathermap query
                final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";

                //url creation class
                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .build();

                //create a new url object with all the parameters set above
                URL url = new URL(builtUri.toString());

                //create a log of verbose type for debugging purposes
                //Log.v(LOG_TAG ,"Built Uri:" + url);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                //create a log of type verbose with the json string for debugging purposes
                //Log.v(LOG_TAG, "Forecast JSON String: " + forecastJsonStr);

                //create a log output to make sure the String[] is returning the right strings
                String[] wForecast = getWeatherDataFromJson(forecastJsonStr,7);
                for(int i = 0; i < wForecast.length;i++){
                    //Log.v(LOG_TAG, "Forecast for: " + wForecast[i]);
                }

                //call the json string parsing method and the json object parser to get the array of
                //strings with the forecast for the next 7 days
                return getWeatherDataFromJson(forecastJsonStr,7);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error parsing JSON string", e);
                //if there was a problem parsing the json file with the weekly forecast
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
        }//end of doInBackground method


        @Override
        protected void onPostExecute(String[] result) {
            if(result != null){
                arrayAdapter.clear();
                for(String dayForecastStr:result){
                    arrayAdapter.add(dayForecastStr);
                }
            }
        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
                 * so for convenience we're breaking it out into its own method now.
                 */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }


        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }


        /**
        * Take the String representing the complete forecast in JSON Format and
        * pull out the data we need to construct the Strings needed for the wireframes.
        *
        * Fortunately parsing is easy:  constructor takes the JSON string and converts it
        * into an Object hierarchy for us.
        */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStrs;
        }//end of getWeatherDataFromJson

    }//end of class FetchWeather

}//end of Fragment class
