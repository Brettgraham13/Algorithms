package audiofingerprinter;

import java.io.File;
import java.util.*;

/**
 * Fingerprints audiofiles
 */
public class Shazam implements AudioFingerprinter {

    private static final long FUZ_FACTOR = 2;
    //Frequency ranges where the most common sounds in music occur
    static final int[] RANGE = new int[]{40, 80, 120, 180, 300};
    //Initializes songDatabase
    SongDatabase songDatabase;

    /**
     * Constructor
     * @param songDatabase
     */
    public Shazam(SongDatabase songDatabase){
        this.songDatabase = songDatabase;
    }

    /**
     * Given a 2D array of frequency information, returns a 2D array of keypoints (called 'points'
     * @param results, an array of frequency data. The first index corresponds with a slice of time, the second with the frequency.
     *                 The data is represented as complex numbers with interleaved real and imaginary parts. For example, to get the
     *                 magnitude of a specific frequency:
     *                      double re = results[time][2*freq];
     *                      double im = results[time][2*freq+1];
     *                      double mag = Math.log(Math.sqrt(re * re + im * im) + 1);
     * @return
     */
    public long[][] determineKeyPoints(double[][] results) {
        //A 2D array that takes in the length of the inputted 2D array and the length of the freq range array
        long[][] points = new long[results.length][RANGE.length];

        for (int t = 0; t < results.length; t++) {
            //An array the size of the freq range array that stores the integer values for the keypoints
            double[] highscores = new double[RANGE.length];
            for (int freq = 30; freq < 300 ; freq++) {
                //Finds the real number for the magnitude
                double re = results[t][2*freq];

                //Finds the imaginary number for the magnitude
                double im = results[t][2*freq+1];

                //Finds the magnitude of a specific frequency
                double mag = Math.log(Math.sqrt(re * re + im * im) + 1);
                //System.out.println(mag);

                // Find out which range we are in:
                int index = getIndex(freq);

                //Checks if the new magnitude is greater than the previous high, then adds it to the 2D keypoints array
                if (mag > highscores[index]){
                    highscores[index] = mag;
                    points[t][index] = freq;
                }
            }
        }
        return points;
    }

    /**
     * Gets the index number of the inputted frequency
     * @param freq
     * @return
     */
    public int getIndex(int freq) {
        int i = 0;
        while (RANGE[i] < freq)
            i++;
        return i;
    }

    /**
     * Gets the songDatabase
     * @return
     */
    public SongDatabase getSongDB(){
        return this.songDatabase;
    }

    /**
     * Method that is given a file object to recognize
     * Gets the rawdata from the file and then returns it after running through the recognize method
     * @param fileIn
     * @return
     */
    public List<String> recognize(File fileIn){
        byte[] audioFile = songDatabase.getRawData(fileIn);
        return recognize(audioFile);
    }

    /**
     * Given an array of songs, this method will return an arraylist of song names with matching fingerprints
     * @param audioData array of bytes representing a song
     * @return
     */
    public List<String> recognize(byte[] audioData){

        //A 2D array that stores the frequency domain of the audioData from the songDatabase
        double[][] fft = songDatabase.convertToFrequencyDomain(audioData);

        //A 2D array that stores the keypoints and frequencies of the audioData
        long[][] keyPts = determineKeyPoints(fft);

        //HashMap that takes in an integer (songID) as its key, and another HashMap as its value that
        //stores the keypoints of that songID and the frequency
        HashMap<Integer,HashMap<Integer,Integer>> similarities = new HashMap<Integer, HashMap<Integer, Integer>>();

        for (int t =0; t < keyPts.length; t++){
            List<DataPoint> matches = songDatabase.getMatchingPoints(hash(keyPts[t]));

            if (matches != null){
                for (int i =0; i< matches.size(); i++) {
                    DataPoint specificMatch = matches.get(i);
                    int timeOffset = specificMatch.getTime() - t;
                    int songID = specificMatch.getSongId();
                    HashMap<Integer, Integer> songSimilarity = similarities.get(songID);

                    //Checks if the HashMap for the specific songID, if null, it creates a new HashMap
                    if (songSimilarity == null) {
                        songSimilarity = new HashMap<>();
                    }

                    //Gets the number of times the songID occurred
                    Integer timesOccurred = songSimilarity.get(timeOffset);

                    //Checks if this is the first time the song has occurred, if so, it changes its value to 0
                    if (timesOccurred == null) {
                        timesOccurred = 0;
                    }

                    //Increments times occurred after it has been confirmed as not null
                    int incrementTimesOccurred = timesOccurred + 1;


                    //Contains the offSet and the time it occurred calculated with the Shazam article formula
                    songSimilarity.put(timeOffset, incrementTimesOccurred);

                    //Adds to the Hashmap of a HashMap that contains the songID as a key, and the songSimilarity HashMap as its values
                    similarities.put(songID, songSimilarity);

                }
            }
        }

        List<SongMatch> rankedWithNames = new ArrayList<>();
        List<String>  finalRanked = new ArrayList<>();

        for (Map.Entry<Integer,HashMap<Integer,Integer>> songIDMaps : similarities.entrySet()) {
            int maxFreq = getMaximum(songIDMaps.getValue());
            int identificationNumber = songIDMaps.getKey();
            SongMatch songMatch = new SongMatch(songDatabase.getSongName(identificationNumber), maxFreq);

            //Adds the matching song name from the songDatabase to the rankedWithNames arraylist
            rankedWithNames.add(songMatch);

        }
        //Calls the collections method to sort the arraylist in descending order from lowest to highest integers
        Collections.sort(rankedWithNames);
        //Calls the collections method 'reverse' to flip the order from highest to lowest (descending)
        Collections.reverse(rankedWithNames);

        //Runs through each of the names in the arraylist rankedWithNames and adds them to the last arraylist 'finalRanked'
        for(SongMatch matches : rankedWithNames){
            finalRanked.add(matches.toString());
        }
        return finalRanked;
    }//End of recognize method

    /**
     * Assuming that this is not done in a 'deaf room' this method uses a formula from the Shazam article that
     * factors in a 'FUZ_FACTOR' to simulate noise
     * @param points array of key points for a particular slice of time. Must be at least length 4.
     * @return
     */
    public long hash(long[] points){
        long p1 = points[0];
        long p2 = points[1];
        long p3 = points[2];
        long p4 = points[3];

        return (p4 - (p4 % FUZ_FACTOR)) * 100000000 + (p3 - (p3 % FUZ_FACTOR))
                * 100000 + (p2 - (p2 % FUZ_FACTOR)) * 100
                + (p1 - (p1 % FUZ_FACTOR));
    }

    /**
     * Gets the maximum number from the inputted HashMap
     * @param similaritiesMap
     * @return
     */
    public int getMaximum(HashMap<Integer,Integer> similaritiesMap){
        int maxNum = 0;

        for (Map.Entry<Integer,Integer> pairs: similaritiesMap.entrySet()){
            if (pairs.getValue() > maxNum){
                maxNum = pairs.getValue();
            }
        }
        return maxNum;
    }
}
