package com.meteor.winter.io;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@AllArgsConstructor
public class ResourceResolver {
    private String base; // 指定的包名

    // 扫描包下所有文件
    public <T> List<T> scan(Function<Resource,T> mapper)  {
        List<T> ts = new ArrayList<>();
        String path = base.replace(".", "/");
        URI uri = null;
        try {
            uri = ClassLoader.getSystemResource(path).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Path root = null;
        // 如果资源在jar中的话，获取与之关联的文件目录
        if(uri.toString().startsWith("jar:")){
            try {
                root = FileSystems.getFileSystem(uri).getPath(path);
            }catch (FileSystemNotFoundException fileSystemNotFoundException){
                // 如果文件系统不存在的话，新建
                try {
                    root = FileSystems.newFileSystem(uri,new HashMap<>())
                            .getPath(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }else root = Paths.get(uri);
        try(Stream<Path> walk = Files.walk(root)){
            // 过滤出非目录文件
            walk.filter(Files::isRegularFile).forEach(file->{
                String filePath = file.toString();
                String fileName = filePath
                        .substring(filePath.replace("\\",".").indexOf(base));
                Resource resource = new Resource(filePath,fileName);
                ts.add(mapper.apply(resource));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ts;
    }

}
