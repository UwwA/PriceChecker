package pricechecker;

import java.awt.Desktop;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import my.Frame.NewJFrame;

public class PriceChecker {

    private static JProgressBar progressBar = new JProgressBar();

    public static void main(String[] args) {
        //Initializes the program
        NewJFrame frame = new NewJFrame();
        frame.setVisible(true);

    }

    static class BackgroundWorker extends SwingWorker<Void, Void> {

        private String[] searches;
        
        //Takes a string array as an argument
        public BackgroundWorker(String[] searches) {
            this.searches = searches;
        }

        @Override
        protected Void doInBackground() throws Exception {
            //Array used for search terms
            String[] search = new String[searches.length];
            URL url;
            InputStream is = null;
            BufferedReader br;
            String line;
            //Prints necessary HTML
            PrintWriter writer = new PrintWriter("prices.html", "UTF-8");
            writer.println("<!doctype html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<script src=\"sorttable.js\"></script><meta charset='utf-8'");
            writer.println("<title>Pricelist</title>");
            writer.println("<link rel='stylesheet' href='style.css'>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("<table class='sortable'>");
            writer.println("<thead><tr><th>Name</th><th>Price</th></tr></thead>");
            //Creates JFrame containing the progressbar            
            JFrame progressFrame = new JFrame("Samlar data...");
            progressFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
            progressFrame.setLocationRelativeTo(null);
            progressFrame.add(progressBar);
            progressFrame.setSize(300, 100);
            progressFrame.setVisible(true);
            //Calculates how many percents a single item is worth towards overall progress
            double perItem = (double) 100 / (double) searches.length;            
            double percent = 0;

            for (int i = 0; i < searches.length; i++) {
                //Duplicates the searches array and cleans it for URL usage
                search[i] = searches[i];
                search[i] = search[i].replaceAll(" ", "%20");
                search[i] = search[i].replaceAll("\t", "%20");
                search[i] = search[i].replaceAll("&", "%26");
                try {
                    //The current URL
                    url = new URL("http://www.prisjakt.nu/search.php?s=" + search[i]);
                    //Updates progress
                    percent = percent + perItem;
                    setProgress((int) percent);
                    //Reads the HTML page
                    is = url.openStream();  
                    br = new BufferedReader(new InputStreamReader(is));
                    //Regex pattern for finding the relevant string
                    Pattern pattern = Pattern.compile("Bästa pris i lager: \\d{2,4}:-");
                    //Reads lines from downloaded HTML
                    while ((line = br.readLine()) != null) {
                        //If we're on the right line
                        if (line.contains("class=\"price") == true) {
                            //Cleans up HTML
                            line = line.replaceAll("&nbsp;", "");
                            Matcher m = pattern.matcher(line);
                            //If pattern matches, clean up string and print HTML
                            if (m.find()) {
                                String someDigits = m.group(0);
                                someDigits = someDigits.replaceAll("[^0-9]+", "");
                                writer.println("<tr><td style='width:100%;'>" + searches[i] + "</td><td><a href='" + url + "'>" + someDigits + "</a></td></tr>");
                            }
                            //Cancels reading of unnecessary lines
                            break;
                        }
                    }
                } catch (MalformedURLException mue) {
                    mue.printStackTrace();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
            //Hides the progressframe and displays the created HTML file to the user
            progressFrame.setVisible(false);
            writer.println("</body>");
            writer.println("</html>");
            writer.close();
            File htmlFile = new File("prices.html");
            Desktop.getDesktop().browse(htmlFile.toURI());
            return null;
        }

    }

    //Upon execution starts the swingworker
    public static void CheckPrice(String[] arg) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        BackgroundWorker worker = new BackgroundWorker(arg);
        //Update progressbar upon progress change
        PropertyChangeListener listener
                = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if ("progress".equals(event.getPropertyName())) {
                    progressBar.setValue((Integer) event.getNewValue());
                }
            }
        };
        worker.addPropertyChangeListener(listener);
        worker.execute();

    }
}
