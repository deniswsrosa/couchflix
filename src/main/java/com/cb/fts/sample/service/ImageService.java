package com.cb.fts.sample.service;

import com.cb.fts.sample.entities.vo.CoverVo;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Install chrome driver with:
 * brew cask install chromedriver
 */
@Component
public class ImageService {

    private static final String prefix = "https://www.google.com/imgres?imgurl=";
    private static final String chromeDriverPath = "/Users/denisrosa/Downloads/chromedriver";

    public CoverVo getImg(String words) throws Exception {

        words = Arrays.asList(words.split(" ")).stream().collect(Collectors.joining(","));

        try {
            ChromeOptions options = new ChromeOptions();
            // setting headless mode to true.. so there isn't any ui
            options.setHeadless(true);

            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
            // Create a new instance of the Chrome driver
            WebDriver driver = new ChromeDriver(options);
            driver.get("https://www.google.com/search?hl=en&tbm=isch&q=" + words + "+movie&oq=star");
            driver.manage().timeouts().implicitlyWait(7, TimeUnit.SECONDS);

            List<WebElement> elements = driver.findElements(By.tagName("a"));

            for (WebElement element : elements) {

                String href = element.getAttribute("href");
                if (href != null && href.startsWith(prefix)) {
                    String url = URLDecoder.decode(element.getAttribute("href").replace(prefix, ""), "UTF-8");

                    if (url.indexOf("&imgrefurl")!= -1) {
                        url = url.substring(0, url.indexOf("&imgrefurl"));
                    }

                    CoverVo coverVo = new CoverVo(url);
                    driver.close();
                    return coverVo;
                }
            }

            return new CoverVo("default.jpg");
        } catch (Exception e) {
            e.printStackTrace();
            return new CoverVo("default.jpg");
        }
    }

}
