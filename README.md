# CS 501 Individual Assignment 4 Question 3 — Temperature Dashboard

## Explanation
The **Temperature Dashboard** simulates real-time temperature readings every 2 seconds and displays them reactively using **ViewModel** and **StateFlow**. It keeps the last 20 readings and shows:
- A simple **line chart** visualization  
- A scrolling list of **timestamps and values**  
- **Current, average, minimum, and maximum** temperature stats  
- A **Pause/Resume** button to control data generation  

## How to Use
1. Run the app — readings will begin automatically and appear in the chart and list.  
2. Use the **Pause/Resume** button in the top bar to stop or restart the simulated data stream.  
3. Observe the summary stats and chart update in real time as new data arrives.  

## Implementation
- **ViewModel (`TemperatureViewModel`)**: Uses a coroutine to simulate sensor readings every 2 seconds, stores the last 20 values in a `MutableStateFlow`, and manages pause/resume state.  
- **UI (`AppRoot` & `Dashboard`)**: Displays live stats, chart, and readings list using **Compose** and **Material 3**. The line chart is drawn with a **Canvas** to visualize recent data.  
- **Reactivity**: All UI elements automatically update when the `StateFlow` changes.
