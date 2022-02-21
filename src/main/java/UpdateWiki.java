import Config.LoadYaml;
import Model.JSonComposer;
import Model.WikiAuthentication;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class UpdateWiki {
    private static Reader reader;
    private static LoadYaml loadYaml = new LoadYaml();

    static {
        try {
            reader = new Reader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // Type in username and password
        Scanner keyboard = new Scanner(System.in);

        //Get username and password to access wiki
        System.out.print("Please enter username to access wiki: " );
        String username = keyboard.nextLine();
        System.out.print("Please enter password: " );
        String password = keyboard.nextLine();
        WikiAuthentication wikiAuthentication = new WikiAuthentication(username, password);
        JSonComposer jSonComposer = new JSonComposer(wikiAuthentication);

        System.out.print("Please enter month (integer): " );
        int month = keyboard.nextInt();

        Map<String, String> reportResult = reader.coverageReportReader(month);
        System.out.println("Coverage reports result: " + reportResult.toString());

        System.out.print("Press yes if you want to post this to wikipage: " );
        String consent = keyboard.next();
        if(consent.equals("yes") || consent.equals("Yes")){
            String coverageFile = loadYaml.getConfig("coverage");
            jSonComposer.postNewJsonFile(coverageFile, reportResult);
        } else {
            System.out.println("Information is not posted." );
        }

        Map<String, String> reportResult2 = reader.newAutomatedReportReader(month);
        System.out.println("Newly automated reports result: " + reportResult2.toString());

        System.out.print("Press yes if you want to post this to wikipage: " );
        String consent2 = keyboard.next();
        if(consent2.equals("yes") || consent2.equals("Yes")){
            String newFile = loadYaml.getConfig("new");
            jSonComposer.postNewJsonFile(newFile, reportResult);
        } else {
            System.out.println("Information is not posted." );
        }

        keyboard.close();
        clearScreen();
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }



}
