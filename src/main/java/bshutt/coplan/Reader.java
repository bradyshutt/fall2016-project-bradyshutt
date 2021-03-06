package bshutt.coplan;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Reader {

    private BufferedReader bufferReader;
    private ReaderStrategy readerStrategy;
    private boolean continueReading = true;

    public Reader(ReaderStrategy readerStrategy) {
        this.readerStrategy = readerStrategy;

        InputStreamReader isr = new InputStreamReader(System.in);
        bufferReader = new BufferedReader(isr);
    }

    void listen(Callback cb) {
        String nextInput;
        this.continueReading = true;
        while (continueReading) {
            try {
                if ((nextInput = bufferReader.readLine()) != null) {
                    Request req = this.readerStrategy.interpret(nextInput);
                    cb.cb(req);
                }
            } catch (Exception exception) {
                System.err.println(exception);
                //exception.printStackTrace();
            }
        }
    }

    void stopReading() {
        this.continueReading = false;
    }

}
