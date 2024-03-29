package mav.goode;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DownloadManager {
       final static List<String> downloads;
       static Connection connection;
       static Path destination,source;

   static  {

       try {
           connection = DriverManager
                   .getConnection("jdbc:mysql://localhost/gdownloader"
                           ,"root","goodie");
       } catch (SQLException throwables) {
           throwables.printStackTrace();
       }
        downloads= getFromDatabase();
        source= getFolderPath();
        destination=getFolderPath();

   }



    public static void main(String[] args) throws  Exception {

        List.of(args).stream()
               .forEach(arg-> run(arg));
    }

    private static void run(String arg)  {
        switch ( arg ){
//            case "help" : String dashboard = """
//               Switch Options
//               delDuplicate - to remove folder duplicates
//               delPrevious - to delete already added to Database List
//               clean - to execute the above two at once
//               update - to add downloaded files to DB List
//               move - to move the files to destination
//               default case does all the above operations
//
//               """;
//               System.out.println(dashboard);break;
            case "delDuplicate" : deleteDuplicates();break;
            case "delPrevious" : deletePreviouslyAddedDownloads();break;
            case "clean" : clean();break;
            case "update" : updateDownloadsDbList();break;
            case "move" : moveFilesToDestination();break;
            default :
                System.out.println("default case executing ....");
                    clean();
                    updateDownloadsDbList();
                    moveFilesToDestination();

        }
    }

    private static void deleteDuplicates()  {
        Stream<Path> downloadedFiles = null;
        try {
            downloadedFiles = Files.walk(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadedFiles.forEach(file-> deleteIfDuplicate(file));
    }
    private static void deletePreviouslyAddedDownloads()  {
        Stream<Path> downloadedFiles = null;
        try {
            downloadedFiles = Files.walk(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadedFiles.forEach(file-> deleteIfPreviouslyAdded(file));
    }

    private static void clean() {
        Stream<Path> downloadedFiles = null;
        try {
            downloadedFiles = Files.walk(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadedFiles.forEach(file-> {
                                deleteIfDuplicate(file);
                                deleteIfPreviouslyAdded(file);
                    });
    }
    static void updateDownloadsDbList() {
        Stream<Path> folderFiles = null;// get directory containing new downloads
        try {
            folderFiles = Files.walk(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
        folderFiles.forEach(file ->insertIntoDatabase(file));    // update DB with new records

    }

    private static void deleteIfPreviouslyAdded(Path downloadedFile)  {
       if(Files.isDirectory(downloadedFile))return;
       String fileName = downloadedFile.getFileName().toString();

        if (downloads.contains(fileName)) {
              try {
                   Files.deleteIfExists(downloadedFile);

               } catch (IOException e) {
                   e.printStackTrace();
               }
       }


   }


    private static void moveFilesToDestination() {
        String filename = createDestinationFolder();
        destination= destination.resolve(filename);
        try {
            Files.createDirectory(destination);
        } catch (IOException e) {
            //e.printStackTrace();
        }
        System.out.println("moving files to :"+destination);
       try {
            Stream<Path>downloadedFiles = Files.walk(source);
            downloadedFiles.forEach(file-> {
                try {
                    if(Files.exists(file))Files.move(file,destination.resolve(file.getFileName()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String createDestinationFolder() {
        Path path=null;
        try {
            Stream<Path> files= Files.list(destination);
                path =files.filter( Files::isDirectory)
                        .filter(file-> file.getFileName().toString().contains("-kids"))
                        .reduce(destination.resolve("1-kids"),(directory1,directory2)->{
            String folder1= directory1.getFileName().toString();
            String folder2= directory2.getFileName().toString();
            String prefix1 = folder1.split("-")[0], prefix2 =folder2.split("-")[0];

            if( prefix1.compareTo(prefix2) < 0) {

                int x = Integer.parseInt(prefix1)+1;
                String filename= x + "-kids";

                return Paths.get(filename);
            }
            else{
                 int x = Integer.parseInt(prefix2)+1;
                String filename= x + "-kids";
                return Paths.get(filename);
            }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }


        return path.getFileName().toString();

    }

    private static void insertIntoDatabase(Path file) {
        try {
            if(Files.isDirectory(file))return;
            PreparedStatement statement= connection.prepareStatement("insert into downloads values (?)");
            statement.setString(1,file.getFileName().toString());
            System.out.println(String.valueOf(statement));
            statement.executeUpdate();
        } catch (SQLException throwables) {
            ;
            if(throwables.getMessage().contains("Incorrect string value:")) {

                String suffix = file.getFileName().toString();
                suffix =suffix.substring(suffix.lastIndexOf('.'));
                try {
                    Path rename = file.getParent().resolve( file.getFileName().toString().substring(0,20) + "-renamed"+suffix);
                    Files.move(file,rename);
                    System.out.println("renamed File :" +file + " to " + rename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
           // throwables.printStackTrace();
        }
    }




    private static List <String>getFromDatabase()  {
        List<String> list = new ArrayList<>();

        try {
            PreparedStatement statement =connection.prepareStatement("select * from downloads");

            ResultSet resultSet = statement.executeQuery();
            if(resultSet!=null)
            while (resultSet.next()){
               list.add(resultSet.getString("filename"));
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        return list;
    }
    private static void upDateDatabase(Path file){
        String fileName = file.getFileName().toString();
        try {
            PreparedStatement statement =connection.prepareStatement("insert into downloads values (?)");
            statement.setString(0,fileName);

            statement.executeUpdate();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    private static Path getFolderPath(){

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")+"\\Downloads\\Video"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            //File selectedFile = fileChooser.getSelectedFile();
            //System.out.println("Selected file: " + selectedFile.getAbsolutePath());

            return  Paths.get(fileChooser.getSelectedFile().getAbsolutePath());
        }
       else return null;
    }
    private static void deleteIfDuplicate(Path downloadedFile)  {
        if(Files.isDirectory(downloadedFile))return;
        String fileName = downloadedFile.getFileName().toString();
        fileName = fileName.substring(0,fileName.lastIndexOf('.'));
        String finalFileName = fileName;
        try {
            Files.list(downloadedFile.getParent())
                           .forEach(file-> {
                               try {
                                   if(Files.exists(file))
                                   if(Files.isSameFile(downloadedFile,file));
                                   else
                                       if(file.getFileName().toString().contains(finalFileName)&& (file.getFileName().toString().length()>finalFileName.length()) )
                                   {
                                       System.out.println("Duplicate file:"+downloadedFile+" <--->"+file);
                                       Files.deleteIfExists(file);
                                       System.out.println("Deleted ");
                                   }

                               } catch (IOException e) {
                                  // e.printStackTrace();
                               }
                           });
        } catch (IOException e) {
           // e.printStackTrace();
        }


    }

}
