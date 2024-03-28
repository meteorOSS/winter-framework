package com.meteor.winter.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlUtil {
    public static Map<String,Object> loadYaml(String path){
        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions dumperOptions = new DumperOptions();
        Representer representer = new Representer(dumperOptions);
        Yaml yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, new NoImplicitResolver());
        return ClassPathUtils.readInputStream(path,(input)->{
            return (Map<String,Object>) yaml.load(input);
        });
    }

    // yaml取出会是树形结构，将他打平为 "x.y.z" 这样的格式
    public static Map<String,Object> loadYamlAsPlainMap(String path){
        Map<String, Object> objectMap = loadYaml(path);
        Map<String,Object> linkMap = new LinkedHashMap<>();
        handler(objectMap,"",linkMap);
        return linkMap;
    }

    private static void handler(Map<String,Object> source,String prefix,Map<String,Object> save) {
        for (String s : source.keySet()) {
            Object o = source.get(s);
            if(o instanceof Map){
                handler((Map<String, Object>) o,s+".",save);
            }else if(o instanceof List){
                save.put(prefix+s,o);
            }else save.put(prefix+s,o.toString());
        }
    }
}

class NoImplicitResolver extends Resolver {

    public NoImplicitResolver() {
        super();
        super.yamlImplicitResolvers.clear();
    }
}