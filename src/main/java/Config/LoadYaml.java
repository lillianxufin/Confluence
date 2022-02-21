package Config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/*
   Class to load config file.
 */
public class LoadYaml {
    /**
     * Put config in a map object.
     *
     * @return
     * @throws IOException
     */
    private Map loadYamlFile() throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream in = LoadYaml.class.getResourceAsStream("/config.yaml")) {
            Map obj = yaml.load(in);
            return obj;
        }
    }

    public String getConfig(String key) throws IOException {
        Map obj = loadYamlFile();
        return obj.get(key).toString();
    }

}
