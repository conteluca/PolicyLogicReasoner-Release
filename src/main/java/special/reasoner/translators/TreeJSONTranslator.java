package special.reasoner.translators;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import special.model.ORNODE;

import java.io.FileWriter;
import java.io.IOException;

public class TreeJSONTranslator {
    private final ORNODE policy;
    public TreeJSONTranslator(ORNODE policy) {
        this.policy=policy;
    }
    public String translate(){
        return toHumanReadable(this.policy.toJson());
    }
    public boolean save(String json,String file){
        try {
            FileWriter myWriter = new FileWriter(file);
            myWriter.write(json);
            myWriter.close();
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return false;
        }
    }
    private String toHumanReadable(String json) {
        try {
            // Inizializza ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);


            // Parsa la stringa JSON in un JsonNode
            JsonNode rootNode = mapper.readTree(json);

            // Converte il JsonNode in una stringa formattata
            return mapper.writeValueAsString(rootNode);


        } catch (JsonParseException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

}
