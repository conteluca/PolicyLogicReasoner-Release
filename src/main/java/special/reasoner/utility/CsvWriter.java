package special.reasoner.utility;


import special.model.PrivacyPolicy;

import java.io.FileWriter;
import java.io.IOException;
public class CsvWriter {
    private final FileWriter writer;

    public CsvWriter(String fileName) throws IOException {
        writer = new FileWriter(fileName);
    }

    public void writeHeader(String[] headers) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < headers.length-1; i++) {
            stringBuilder.append(headers[i]).append(";");
        }
        stringBuilder.append(headers[headers.length-1]).append("\n");
        writer.write(stringBuilder.toString());

    }

    public void writeRow(PrivacyPolicy privacyPolicy) throws IOException {
        writer.write(privacyPolicy.toString()+"\n");
    }

    public void close() throws IOException {
        writer.close();
    }
}