package com.tastedivekafka.cache;

import java.awt.Image;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.time.Duration;
import java.net.URI;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

/* Cache global para cargar las imagenes en sus respectivos placeholders
    evitando descargar la misma imagen varias veces */

public class ImageCache {
    private final LoadingCache<String, Image> cache;
    
    public ImageCache() {
        cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(30))
            .refreshAfterWrite(Duration.ofHours(2))
            .build(this::loadImageFromUrl);
    }

    public Image loadImage(String url) {
        try {
            return cache.get(url);
        } catch (Exception e) {
            System.err.println("Error cargando la imagen: " + url + " -> " + e.getMessage());
            return null;
        }
    }

    private Image loadImageFromUrl(String urlString) throws IOException {
        try{
            URI uri = URI.create(urlString);
            return ImageIO.read(uri.toURL()); 
        } catch (Exception e){
            System.err.println("URL invalida: " + urlString);
            return null;
        }
    }
}
