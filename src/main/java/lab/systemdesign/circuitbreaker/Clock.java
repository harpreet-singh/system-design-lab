package lab.systemdesign.circuitbreaker;

public interface Clock {
    long millis();

    static Clock system() {
        return System::currentTimeMillis;
    }
}
