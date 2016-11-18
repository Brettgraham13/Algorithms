package audiofingerprinter;

/**
 * Created by Brett Graham on 12/4/2015.
 */
public class SongMatch implements Comparable<SongMatch> {

    private String songName;
    private int matchNum;

    /**
     * Constructor
     * @param songName
     * @param matchNum
     */
    public SongMatch(String songName,int matchNum){
        this.songName = songName;
        this.matchNum = matchNum;
    }

    /**
     * Compares song match objects
     * @param match
     * @return
     */
    @Override
    public int compareTo(SongMatch match){
        if (this.matchNum > match.matchNum){
            return 1;
        } else if (this.matchNum < match.matchNum){
            return -1;
        } else{
            return 0;
        }
    }

    /**
     * toString method that returns the songName and the matching number in an orderly format
     * @return
     */
    @Override
    public String toString(){
        return songName + ":   " + matchNum;

    }
}
