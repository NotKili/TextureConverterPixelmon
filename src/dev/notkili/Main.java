package dev.notkili;

import com.google.gson.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

public class Main {

    private static HashSet<String> alreadyConvertedPokemon = new HashSet<>();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static int TEXTURE_ERROR_COUNT = 0;
    private static int SPRITE_ERROR_COUNT = 0;
    private static int FILE_ERROR_COUNT = 0;
    private static ArrayList<String> erroredTextures = new ArrayList<>();
    private static ArrayList<String> erroredSprites = new ArrayList<>();
    private static ArrayList<String> erroredFiles = new ArrayList<>();

    // Arg 0 == Path of stats.json
    // Arg 1 == Path of textures
    // Arg 2 == Path of Sprites
    // Arg 3 == (Optional) Output-Path
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String statsPath;
        String pixelmonTexturesPath;
        String pixelmonSpritesPath;
        String outputPath;
        PathValues tempResult;

        System.out.println("Hey, please follow these steps to convert your existing texturepacks into the new pixelmon 9.0.0 stats-files!" +
                "\n\nDisclaimer: This won't work for pokemon who previously had forms that are now palettes, i.e. Vivillon, Florges, Shellos et cetera" +
                "\nMimikyu needs 'Disguised'/'Busted' to be added as a palette" +
                "\nThese pokemon must be changed manually in order for the program to recognize them" +
                "\nThis also applies to emissive pokemon. Those need to be taken out of their folder and placed in the normal texture folder for the program to parse them properly" +
                "\n(Or just remove them temporarily)" +
                "\n\nSupported is:" +
                "\n- All known forms (i.e. dynamax, mega, arceus ..." +
                "\n- Genders (Male, Female, All (wildcard)" +
                "\n- Shiny / Non - Shiny Pokemon, Shinies will be saved as palette: <texturename-shiny>" +
                "\n- Sprites (all above should apply)" +
                "\n\n\nMade by: NotKili (NotKili#1200)" +
                "\nPlease send bug-reports my way (preferably over discord) when any happen! (Some will still exist, my sample with ~500 textures works flawless apart from vivillions, mareeps & other previously mentioned pokemon");

        while (true) {
            System.out.print("\nPlease enter the path of the stats folder: ");
            statsPath = scanner.nextLine();

            if ((tempResult = validatePath(statsPath)) == PathValues.SUCCESS) {
                System.out.printf(tempResult.getDescription(), statsPath);
                break;
            } else {
                System.err.println(tempResult.getDescription() + "\n");
                continue;
            }
        }

        while (true) {
            System.out.print("\nPlease enter the path of the custom texture's folder: ");
            pixelmonTexturesPath = scanner.nextLine();

            if ((tempResult = validatePath(pixelmonTexturesPath)) == PathValues.SUCCESS) {
                System.out.printf(tempResult.getDescription(), pixelmonTexturesPath);
                break;
            } else {
                System.err.println(tempResult.getDescription() + "\n");
                continue;
            }
        }

        while (true) {
            System.out.print("\nPlease enter the path of the custom sprites folder: ");
            pixelmonSpritesPath = scanner.nextLine();

            if ((tempResult = validatePath(pixelmonSpritesPath)) == PathValues.SUCCESS) {
                System.out.printf(tempResult.getDescription(), pixelmonSpritesPath);
                break;
            } else {
                System.err.println(tempResult.getDescription() + "\n");
                continue;
            }
        }

        while (true) {
            System.out.print("\nPlease enter the path of the output folder: ");
            outputPath = scanner.nextLine();

            if ((tempResult = validatePath(outputPath)) == PathValues.SUCCESS) {
                System.out.printf(tempResult.getDescription(), outputPath);
                break;
            } else {
                System.err.println(tempResult.getDescription() + "\n");
                continue;
            }
        }

        convertAll(statsPath, pixelmonTexturesPath, pixelmonSpritesPath, outputPath);

        System.out.println("Finished conversion! Your updated stats-files are now in '" + outputPath + "'");

        System.out.println("\nResults:" +
                "\n" +
                "\n" +
                "\nErrored Textures: " + TEXTURE_ERROR_COUNT);
        for (String texture : erroredTextures) {
            System.out.println("\t- " + texture);
        }

        System.out.println("\n" +
                "\nErrored Sprites: " + SPRITE_ERROR_COUNT);
        for (String sprite : erroredSprites) {
            System.out.println("\t- " + sprite);
        }

        System.out.println("\n" +
                "\nErrored Files (Stat's files unable to fetch): " + FILE_ERROR_COUNT);
        for (String name : erroredFiles) {
            System.out.println("\t- " + name);
        }
    }

    private static PathValues validatePath(String path) {

        if (path.isEmpty()) {
            return PathValues.EMPTY;
        }

        File file = new File(path);

        if (!file.exists()) {
            return PathValues.NOT_EXIST;
        } else {
            if (!file.isDirectory()) {
                return PathValues.NO_DIR;
            }
            return PathValues.SUCCESS;
        }
    }

    private static void convertAll(String statsPath, String pokemonPath, String spritePath, String outputPath) {

        File statsFolder = new File(statsPath);
        File pokemonTextureFolder = new File(pokemonPath);
        File spriteTextureFolder = new File(spritePath);
        File outputFolder = new File(outputPath);

        if (isDir(statsFolder) && isDir(pokemonTextureFolder) && isDir(spriteTextureFolder)) {
            if (!isDir(outputFolder)) {
                if (!outputFolder.mkdirs()) {
                    System.err.println("An error occurred trying to create the output folder. Make sure it exists!");
                    return;
                }
            }

            convertAllPokemon(statsFolder, pokemonTextureFolder, outputFolder);
            convertAllSprites(spriteTextureFolder, outputFolder);
        }
    }

    private static boolean isDir(File file) {
        return file.exists() && file.isDirectory();
    }

    private static void convertAllPokemon(File statsFolder, File pokemonFolder, File outputFolder) {
        String textureName = pokemonFolder.getName();

        for (File pokemonFile : pokemonFolder.listFiles()) {
            if (pokemonFile.isDirectory()) {
                convertAllPokemon(statsFolder, pokemonFile, outputFolder);
            } else {
                if (pokemonFile.getName().endsWith(".png")) {
                    String fileName = pokemonFile.getName().replace(".png", "");

                    PokemonObject currentPokemon = createPokemonObjectFromName(fileName);
                    File pokemonStatFile = findPokemonStatsFile(outputFolder, statsFolder, currentPokemon.getName().toLowerCase());

                    if (pokemonStatFile == null) {
                        FILE_ERROR_COUNT++;
                        System.err.println("Could not locate stats file for '" + currentPokemon + "'");
                        erroredFiles.add(fileName);
                        continue;
                    }

                    if (convertPokemon(pokemonStatFile, textureName, fileName, currentPokemon, outputFolder)) {
                        System.out.println("Converted the texture '" + textureName + "' for " + currentPokemon);
                    } else {
                        TEXTURE_ERROR_COUNT++;
                        erroredTextures.add(textureName + ": " + fileName);
                        System.err.println("An error occurred while trying to convert the texture '" + textureName + "' for " + currentPokemon);
                    }
                } else {
                    System.err.println("Found file with unsupported file extension: '" + pokemonFile.getName() + "'");
                }
            }
        }
    }

    private static void convertAllSprites(File spriteFolder, File outputFolder) {
        for (File spriteFile : spriteFolder.listFiles()) {
            if (spriteFile.isDirectory()) {
                convertAllSprites(spriteFile, outputFolder);
            } else {
                if (spriteFile.getName().endsWith(".png")) {
                    String fileName = spriteFile.getName().replace(".png", "");

                    PokemonObject pokemonObject = createPokemonObjectFromName(fileName);
                    String textureName = spriteFolder.getName();

                    File pokemonStatsFile = findPokemonStatsFile(outputFolder, pokemonObject.name);

                    if (pokemonStatsFile == null) {
                        FILE_ERROR_COUNT++;
                        System.err.println("Could not locate stats file for '" + pokemonObject + "'");
                        erroredFiles.add(fileName);
                        continue;
                    }

                    if (convertSprite(outputFolder, pokemonStatsFile, textureName, fileName, pokemonObject)) {
                        System.out.println("Converted the sprite '" + textureName + "' for " + pokemonObject);
                    } else {
                        SPRITE_ERROR_COUNT++;
                        erroredSprites.add(textureName + ": " + fileName);
                        System.err.println("An error occurred while trying to convert the sprite '" + textureName + "' for " + pokemonObject);
                    }

                } else {
                    System.err.println("Found file with unsupported file extension: '" + spriteFile.getName() + "'");
                }
            }
        }
    }

    private static PokemonObject createPokemonObjectFromName(String name) {
        String[] splittedArray = name.split("-");

        String pokemonName;
        String gender;
        String formName;
        boolean shiny = false;

        if (splittedArray[0].endsWith("female")) {
            gender = "FEMALE";
            pokemonName = splittedArray[0].replace("female", "");
        } else if (splittedArray[0].endsWith("male")) {
            gender = "MALE";
            pokemonName = splittedArray[0].replace("male", "");
        } else {
            gender = "ALL";
            pokemonName = splittedArray[0];
        }

        if (splittedArray.length > 1 && splittedArray[1].equals("shiny")) {
            splittedArray[1] = "";
            shiny = true;
        }

        if (pokemonName.contains("shiny")) {
            pokemonName = pokemonName.replace("shiny", "");
            shiny = true;
        }

        if (pokemonName.equals("ho")) {
            pokemonName = "hooh";
            formName = splittedArray.length > 2 ? joinArray(2, splittedArray) : "";;
        } else if (pokemonName.equals("porygon")) {
            if (splittedArray.length > 1) {
                if (splittedArray[1].equals("z")) {
                    formName = splittedArray.length > 2 ? joinArray(2, splittedArray) : "";
                    pokemonName = "porygon-z";
                } else {
                    formName = joinArray(1, splittedArray);
                }
            } else {
                formName = "";
            }
        } else {
            formName = splittedArray.length > 1 ? joinArray(1, splittedArray) : "";
        }

        if (formName.equals("normal")) {
            formName = "";
        }

        switch (formName) {
            case "alola":
                formName = "alolan";
                break;
            case "galar":
                formName = "galarian";
                break;
        }

        return new PokemonObject(pokemonName, gender, formName, shiny);
    }

    private static boolean convertSprite(File outputFolder, File pokeStatsFile, String textureName, String fileName, PokemonObject pokemon) {
        try {
            JsonObject pokemonObject = JsonParser.parseReader(new BufferedReader(new FileReader(pokeStatsFile))).getAsJsonObject();
            String strippedTexture = textureName.replace("custom-", "");
            String pokemonName = pokemonObject.get("name").getAsString().toLowerCase();

            JsonArray forms = pokemonObject.getAsJsonArray("forms");

            for (int i = 0; i < forms.size(); i++) {
                JsonObject currentForm = forms.get(i).getAsJsonObject();
                String formName = pokemon.getForm();

                if (currentForm.get("name").getAsString().equals(formName)) {
                    JsonArray genderProperties = currentForm.get("genderProperties").getAsJsonArray();
                    String gender = pokemon.getGender();

                    for (int k = 0; k < genderProperties.size(); k++) {
                        JsonObject genderObject = genderProperties.get(k).getAsJsonObject();

                        if (gender.equals("ALL") || genderObject.get("gender").getAsString().equals(gender)) {
                            JsonArray allTextures = genderObject.getAsJsonArray("palettes");

                            for (int j = 0; j < allTextures.size(); j++) {
                                JsonObject currentTexture = allTextures.get(j).getAsJsonObject();

                                if (currentTexture.get("name").getAsString().equals(strippedTexture)) {
                                    currentTexture.addProperty("sprite", "pixelmon:sprite/" + textureName + "/" + fileName + ".png");

                                    if (writeToFile(outputFolder, pokemonObject, pokemonName, pokemon.getName())) {
                                        alreadyConvertedPokemon.add(pokemonName);
                                    } else {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean convertPokemon(File statsFile, String textureName, String fileName, PokemonObject pokemon, File outputFolder) {
        try {
            JsonObject pokemonObject = JsonParser.parseReader(new BufferedReader(new FileReader(statsFile))).getAsJsonObject();
            String strippedTexture = textureName.replace("custom-", "");
            strippedTexture = pokemon.isShiny() ? strippedTexture + "-shiny" : strippedTexture;
            String pokemonName = pokemonObject.get("name").getAsString().toLowerCase();
            int dexNum = pokemonObject.get("dex").getAsInt();

            JsonArray forms = pokemonObject.getAsJsonArray("forms");

            if (pokemonName.equals("pidgeot")) {
                System.out.println("Looking for form: " + pokemon.getForm());
            }

            for (int i = 0; i < forms.size(); i++) {
                JsonObject currentForm = forms.get(i).getAsJsonObject();

                if (pokemonName.equals("pidgeot")) {
                    System.out.println("Found form: " + currentForm.get("name").getAsString());
                }

                if (currentForm.get("name").getAsString().equals(pokemon.getForm())) {
                    JsonArray genderProperties = currentForm.get("genderProperties").getAsJsonArray();
                    String gender = pokemon.getGender();

                    for (int k = 0; k < genderProperties.size(); k++) {
                        JsonObject genderPropertiesObject = genderProperties.get(k).getAsJsonObject();
                        if (genderPropertiesObject.get("gender").getAsString().equals(gender)) {
                            JsonArray currentTextures = genderPropertiesObject.getAsJsonArray("palettes");

                            JsonObject textureToAdd = new JsonObject();
                            textureToAdd.addProperty("name", strippedTexture);
                            textureToAdd.addProperty("texture", "pixelmon:pokemon/" + textureName + "/" + fileName + ".png");

                            currentTextures.add(textureToAdd);

                            if (writeToFile(outputFolder, pokemonObject, pokemonName, String.format("%03d", dexNum))) {
                                alreadyConvertedPokemon.add(pokemonName);
                            } else {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean writeToFile(File outputFolder, JsonObject object, String pokemonName, String dexNum) {
        try {
            File pokemonFile = new File(outputFolder, dexNum + "_" + pokemonName + ".json");
            BufferedWriter writer = new BufferedWriter(new FileWriter(pokemonFile));
            writer.write(gson.toJson(object));
            writer.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static File findPokemonStatsFile(File outputFiles, File statsFiles, String pokemonName) {
        if (alreadyConvertedPokemon.contains(pokemonName)) {
            for (File convertedFile : outputFiles.listFiles()) {
                if (convertedFile.isDirectory()) {
                    findPokemonStatsFile(outputFiles, convertedFile, pokemonName);
                    continue;
                }

                if (convertedFile.getName().contains(pokemonName)) {
                    return convertedFile;
                }
            }
        } else {
            for (File statsFile : statsFiles.listFiles()) {
                if (statsFile.isDirectory()) {
                    findPokemonStatsFile(outputFiles, statsFile, pokemonName);
                    continue;
                }

                if (statsFile.getName().contains(pokemonName)) {
                    return statsFile;
                }
            }
        }
        return null;
    }

    private static File findPokemonStatsFile(File outputFiles, String dexNumber) {
        for (File convertedFile : outputFiles.listFiles()) {

            if (convertedFile.getName().contains(dexNumber)) {
                return convertedFile;
            }
        }

        return null;
    }

    private static String joinArray(int indexFrom, String[] array) {
        StringBuilder result = new StringBuilder();

        for (int i = indexFrom; i < array.length; i++) {
            result.append(array[i]);
        }

        return result.toString();
    }

    private enum PathValues {
        EMPTY("The path can't be empty!"),
        NOT_EXIST("This path does not exist within the system!"),
        SUCCESS("Successfully set path of the folder to '%s'"),
        NO_DIR("This path does not lead to a directory!");

        private String description;

        PathValues(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private static class PokemonObject {
        private String name;
        private String gender;
        private String form;
        private boolean shiny;

        public PokemonObject(String name, String gender, String form, boolean shiny) {
            this.name = name;
            this.gender = gender;
            this.form = form;
            this.shiny = shiny;
        }

        public String getName() {
            return name;
        }

        public String getGender() {
            return gender;
        }

        public String getForm() {
            return form;
        }

        public boolean isShiny() {
            return shiny;
        }

        @Override
        public String toString() {
            return "PokemonObject{" +
                    "name='" + name + '\'' +
                    ", gender='" + gender + '\'' +
                    ", form='" + form + '\'' +
                    ", shiny=" + shiny +
                    '}';
        }
    }
}
