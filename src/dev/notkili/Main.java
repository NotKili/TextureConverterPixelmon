package dev.notkili;

import com.google.gson.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    private static HashSet<String> alreadyConvertedPokemon = new HashSet<>();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static File logFile = new File("log.txt");
    private static File resultFile = new File("result.txt");

    private static BufferedWriter logWriter;
    private static BufferedWriter resultWriter;

    private static int TEXTURE_ERROR_COUNT = 0;
    private static int SPRITE_ERROR_COUNT = 0;
    private static int FILE_ERROR_COUNT = 0;
    private static int EMISSIVE_TEXTURE_ERROR_COUNT = 0;
    private static ArrayList<String> erroredTextures = new ArrayList<>();
    private static ArrayList<String> erroredSprites = new ArrayList<>();
    private static ArrayList<String> erroredEmissiveTextures = new ArrayList<>();
    private static ArrayList<String> erroredFiles = new ArrayList<>();

    private static ArrayList<EmissiveTextures> emissiveTextures = new ArrayList<>();

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

        try {
            logWriter = new BufferedWriter(new FileWriter(logFile));
            resultWriter = new BufferedWriter(new FileWriter(resultFile));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not instantiate log-writer, can't continue");
            return;
        }

        System.out.println("Hey, please follow these steps to convert your existing texturepacks into the new pixelmon 9.0.0 stats-files!" +
                "\n\nDisclaimer: This won't work for pokemon who previously had forms that are now palettes, i.e. Vivillon, Florges, Shellos et cetera" +
                "\nThese pokemon must be changed manually in order for the program to recognize them" +
                "\n\nSupported is:" +
                "\n- All known forms (i.e. dynamax, mega, arceus ..." +
                "\n- Genders (Male, Female, All (wildcard)" +
                "\n- Shiny / Non - Shiny Pokemon, Shinies will be saved as palette: <texturename-shiny> & will have the default shiny particle applied" +
                "\n- Sprites (all above should apply)" +
                "\n- Emissive textures" +
                "\n\nAll textures now follow the base paths (i.e. pixelmon:pokemon/<dex>_<name>/all/<form>/<texture_name>/<textureName>.png)" +
                "\nWith textureName being: [sprite, texture, emissive]" +
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

        logMessage("Finished conversion! Your updated stats-files are now in '" + outputPath + "'");


        logResult("\nResults:" +
                "\n" +
                "\n" +
                "\nErrored Textures: " + TEXTURE_ERROR_COUNT);
        for (String texture : erroredTextures) {
            logResult("\t- " + texture);
        }

        logResult("\n" +
                "\nErrored Sprites: " + SPRITE_ERROR_COUNT);
        for (String sprite : erroredSprites) {
            logResult("\t- " + sprite);
        }

        logResult("\n" +
                "\nErrored Emissive Textures: " + EMISSIVE_TEXTURE_ERROR_COUNT);
        for (String name : erroredEmissiveTextures) {
            logResult("\t- " + name);
        }

        logResult("\n" +
                "\nErrored Files (Stat's files unable to fetch): " + FILE_ERROR_COUNT);
        for (String name : erroredFiles) {
            logResult("\t- " + name);
        }

        try {
            logWriter.close();
            resultWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
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
                    logMessage("An error occurred trying to create the output folder. Make sure it exists!", true);
                    return;
                }
            }

            convertAllPokemon(statsFolder, pokemonTextureFolder, outputFolder);
            convertAllSprites(spriteTextureFolder, outputFolder);
            convertAllEmissiveTextures(outputFolder);
        }
    }

    private static boolean isDir(File file) {
        return file.exists() && file.isDirectory();
    }

    private static void convertAllPokemon(File statsFolder, File pokemonFolder, File outputFolder) {
        String textureName = pokemonFolder.getName();

        for (File pokemonFile : pokemonFolder.listFiles()) {
            if (pokemonFile.isDirectory()) {
                if (pokemonFile.getName().contains("emissive")) {
                    emissiveTextures.add(new EmissiveTextures(pokemonFile, textureName));
                } else {
                    convertAllPokemon(statsFolder, pokemonFile, outputFolder);
                }
            } else {
                if (pokemonFile.getName().endsWith(".png")) {
                    String fileName = pokemonFile.getName().replace(".png", "");
                    fileName = fileName.strip();

                    PokemonObject currentPokemon = createPokemonObjectFromName(fileName);
                    File pokemonStatFile = findPokemonStatsFile(outputFolder, statsFolder, currentPokemon.getName().toLowerCase());

                    if (pokemonStatFile == null) {
                        FILE_ERROR_COUNT++;
                        logMessage("Could not locate stats file for '" + currentPokemon + "'", true);
                        erroredFiles.add(fileName);
                        continue;
                    }

                    if (convertPokemon(pokemonStatFile, textureName, fileName, currentPokemon, outputFolder)) {
                        logMessage("Converted the texture '" + textureName + "' for " + currentPokemon);
                    } else {
                        TEXTURE_ERROR_COUNT++;
                        erroredTextures.add(textureName + ": " + fileName);
                        logMessage("An error occurred while trying to convert the texture '" + textureName + "' for " + currentPokemon, true);
                    }
                } else {
                    logMessage("Found file with unsupported file extension: '" + pokemonFile.getName() + "'", true);
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
                    fileName = fileName.strip();

                    PokemonObject pokemonObject = createPokemonObjectFromName(fileName);
                    String textureName = spriteFolder.getName();

                    File pokemonStatsFile = findPokemonStatsFile(outputFolder, pokemonObject.name);

                    if (pokemonStatsFile == null) {
                        FILE_ERROR_COUNT++;
                        logMessage("Could not locate stats file for '" + pokemonObject + "'", true);
                        erroredFiles.add(fileName);
                        continue;
                    }

                    if (convertSprite(outputFolder, pokemonStatsFile, textureName, fileName, pokemonObject)) {
                        logMessage("Converted the sprite '" + textureName + "' for " + pokemonObject);
                    } else {
                        SPRITE_ERROR_COUNT++;
                        erroredSprites.add(textureName + ": " + fileName);
                        logMessage("An error occurred while trying to convert the sprite '" + textureName + "' for " + pokemonObject, true);
                    }

                } else {
                    logMessage("Found file with unsupported file extension: '" + spriteFile.getName() + "'", true);
                }
            }
        }
    }

    private static void convertAllEmissiveTextures(File outputFolder) {
        for (EmissiveTextures emissiveTextures : emissiveTextures) {
            convertEmissiveTextureFolder(emissiveTextures.getFolder(), outputFolder, emissiveTextures.getTextureName());
        }
    }

    private static void convertEmissiveTextureFolder(File textureFolder, File outputFolder, String textureName) {
        for (File potentialEmissiveTexture : textureFolder.listFiles()) {
            if (potentialEmissiveTexture.isDirectory()) {
                convertEmissiveTextureFolder(potentialEmissiveTexture, outputFolder, textureName);
            } else {
                if (potentialEmissiveTexture.getName().endsWith(".png")) {
                    String fileName = potentialEmissiveTexture.getName().replace(".png", "");
                    fileName = fileName.strip();

                    PokemonObject pokemon = createPokemonObjectFromName(fileName);

                    File pokemonStatsFile = findPokemonStatsFile(outputFolder, pokemon.name);

                    if (pokemonStatsFile == null) {
                        FILE_ERROR_COUNT++;
                        logMessage("Could not locate stats file for '" + pokemon + "'", true);
                        erroredFiles.add(fileName);
                        continue;
                    }

                    if (convertEmissiveTexture(pokemon, textureName, potentialEmissiveTexture.getName(), outputFolder, pokemonStatsFile)) {
                        logMessage("Converted the emissive texture '" + textureName + "' for " + pokemon);
                    } else {
                        EMISSIVE_TEXTURE_ERROR_COUNT++;
                        erroredEmissiveTextures.add(textureName + ": " + fileName);
                        logMessage("An error occurred while trying to convert the sprite '" + textureName + "' for " + pokemon, true);
                    }
                } else {
                    logMessage("Found file with unsupported file extension: '" + potentialEmissiveTexture.getName() + "'", true);
                }
            }
        }
    }

    private static boolean convertEmissiveTexture(PokemonObject pokemon, String textureName, String fileName, File outputFolder, File statsFile) {
        try {
            JsonObject pokemonObject = JsonParser.parseReader(new BufferedReader(new FileReader(statsFile))).getAsJsonObject();
            String strippedTexture = textureName.replace("custom-", "");
            strippedTexture = pokemon.isShiny() ? strippedTexture + "-shiny" : strippedTexture;

            String dexNum = String.format("%03d", pokemonObject.get("dex").getAsInt());
            String name = pokemonObject.get("name").getAsString().toLowerCase(Locale.ROOT);

            JsonArray forms = pokemonObject.getAsJsonArray("forms");

            for (int i = 0; i < forms.size(); i++) {
                JsonObject currentForm = forms.get(i).getAsJsonObject();

                if (isForm(pokemon.getForm(), currentForm.get("name").getAsString(), pokemon.getName())) {
                    JsonArray genderProperties = currentForm.get("genderProperties").getAsJsonArray();
                    String gender = pokemon.getGender();

                    String form = currentForm.get("name").getAsString().equals("") ? "base" : currentForm.get("name").getAsString();

                    for (int k = 0; k < genderProperties.size(); k++) {
                        JsonObject genderObject = genderProperties.get(k).getAsJsonObject();

                        if (gender.equals("ALL") || genderObject.get("gender").getAsString().equalsIgnoreCase(gender)) {
                            JsonArray allTextures = genderObject.getAsJsonArray("palettes");

                            for (int j = 0; j < allTextures.size(); j++) {
                                JsonObject currentTexture = allTextures.get(j).getAsJsonObject();

                                if (currentTexture.get("name").getAsString().equalsIgnoreCase(strippedTexture)) {
                                    String genderString = genderObject.get("gender").getAsString().toLowerCase(Locale.ROOT);
                                    currentTexture.addProperty("emissive", "pixelmon:pokemon/" + dexNum + "_" + name + "/" + genderString + "/" + form + "/"  + textureName + "/emissive.png");
                                    return writeToFile(outputFolder, pokemonObject, statsFile.getName());
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

    private static PokemonObject createPokemonObjectFromName(String name) {

        String[] splittedArray = name.split("-");

        String pokemonName;
        String gender = "";
        String formName;
        boolean shiny = false;


        for (int i = 0; i < splittedArray.length; i++) {
            String current = splittedArray[i];

            if (current.contains("female")) {
                gender = "FEMALE";
                current = current.replace("female", "");
            } else if (current.contains("male")) {
                current = current.replace("male", "");
                gender = "MALE";
            }

            if (current.contains("shiny")) {
                current = current.replace("shiny", "");
                shiny = true;
            }

            splittedArray[i] = current;
        }

        if (gender.equals("")) {
            gender = "ALL";
        }

        pokemonName = splittedArray[0];

        switch (pokemonName) {
            case "ho":
                pokemonName = pokemonName + "oh";
                formName = splittedArray.length > 2 ? joinArray(2, splittedArray) : "";
                ;
                break;
            case "porygon":
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
                break;
            case "kommo":
            case "jangmo":
            case "hakamo":
                pokemonName = pokemonName + "-o";
                formName = splittedArray.length > 2 ? joinArray(2, splittedArray) : "";
                break;
            case "nidoran":
                pokemonName = pokemonName + gender.toLowerCase();
                gender = "ALL";
                formName = splittedArray.length > 1 ? joinArray(1, splittedArray) : "";
                break;
            default:
                formName = splittedArray.length > 1 ? joinArray(1, splittedArray) : "";
                break;
        }

        if ((pokemonName.equals("darmanitan") || pokemonName.equals("555")) && formName.equals("standard")) {
            formName = "";
        }

        switch (pokemonName) {
            case "zygarde":
                formName = formName.isBlank() ? "fifty_percent" : formName;
                break;
            case "mimikyu":
                formName = formName.isBlank() ? "disguised" : formName;
                break;
            case "wishiwashi":
                formName = formName.isBlank() ? "solo" : formName;
                break;
            case "cramorant":
                formName = formName.isBlank() ? "gulping" : formName;
                break;
        }

        if (formName.equals("normal")) {
            formName = "";
        }

        switch (formName) {
            case "alola":
                formName = "alolan";
                break;
            case "galar":
            case "galarstandard":
                formName = "galarian";
                break;
            case "galarzen":
                formName = "galarian_zen";
                break;
            case "incarnate":
                formName = "therian";
                break;
        }

        return new PokemonObject(pokemonName, gender, formName, shiny);
    }

    private static boolean convertSprite(File outputFolder, File pokeStatsFile, String textureName, String fileName, PokemonObject pokemon) {
        try {
            JsonObject pokemonObject = JsonParser.parseReader(new BufferedReader(new FileReader(pokeStatsFile))).getAsJsonObject();
            String strippedTexture = textureName.replace("custom-", "");
            strippedTexture = pokemon.isShiny() ? strippedTexture + "-shiny" : strippedTexture;
            String pokemonName = pokemonObject.get("name").getAsString().toLowerCase();

            String dexNum = String.format("%03d", pokemonObject.get("dex").getAsInt());
            String name = pokemonObject.get("name").getAsString().toLowerCase(Locale.ROOT);

            JsonArray forms = pokemonObject.getAsJsonArray("forms");

            for (int i = 0; i < forms.size(); i++) {
                JsonObject currentForm = forms.get(i).getAsJsonObject();

                if (isForm(pokemon.getForm(), currentForm.get("name").getAsString(), pokemon.getName())) {
                    JsonArray genderProperties = currentForm.get("genderProperties").getAsJsonArray();
                    String gender = pokemon.getGender();

                    for (int k = 0; k < genderProperties.size(); k++) {
                        JsonObject genderObject = genderProperties.get(k).getAsJsonObject();

                        if (gender.equals("ALL") || genderObject.get("gender").getAsString().equalsIgnoreCase(gender)) {
                            JsonArray allTextures = genderObject.getAsJsonArray("palettes");

                            for (int j = 0; j < allTextures.size(); j++) {
                                JsonObject currentTexture = allTextures.get(j).getAsJsonObject();

                                if (currentTexture.get("name").getAsString().equals(strippedTexture)) {
                                    String form = currentForm.get("name").getAsString().equals("") ? "base" : currentForm.get("name").getAsString();
                                    String genderString = genderObject.get("gender").getAsString().toLowerCase(Locale.ROOT);
                                    currentTexture.addProperty("sprite", "pixelmon:pokemon/" + dexNum + "_" + name + "/" + genderString + "/" + form + "/"  + textureName + "/sprite.png");

                                    if (writeToFile(outputFolder, pokemonObject, pokeStatsFile.getName())) {
                                        alreadyConvertedPokemon.add(pokemonName);
                                        return true;
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

            String dexNum = String.format("%03d", pokemonObject.get("dex").getAsInt());
            String name = pokemonObject.get("name").getAsString().toLowerCase(Locale.ROOT);

            JsonArray forms = pokemonObject.getAsJsonArray("forms");

            for (int i = 0; i < forms.size(); i++) {
                JsonObject currentForm = forms.get(i).getAsJsonObject();

                if (isForm(pokemon.getForm(), currentForm.get("name").getAsString(), pokemon.getName())) {
                    JsonArray genderProperties = currentForm.get("genderProperties").getAsJsonArray();
                    String gender = pokemon.getGender();

                    int conversions = 0;

                    for (int k = 0; k < genderProperties.size(); k++) {
                        JsonObject genderObject = genderProperties.get(k).getAsJsonObject();

                        if (genderObject.get("gender").getAsString().equalsIgnoreCase(gender)) {
                            JsonArray currentTextures = genderObject.getAsJsonArray("palettes");

                            JsonObject textureToAdd = new JsonObject();
                            String form = currentForm.get("name").getAsString().equals("") ? "base" : currentForm.get("name").getAsString();
                            String genderString = genderObject.get("gender").getAsString().toLowerCase(Locale.ROOT);

                            textureToAdd.addProperty("name", strippedTexture);
                            textureToAdd.addProperty("texture", "pixelmon:pokemon/" + dexNum + "_" + name + "/" + genderString + "/" + form + "/"  + textureName + "/texture.png");

                            if (pokemon.isShiny()) {
                                textureToAdd.addProperty("particle", "arcanery:shiny");
                            }

                            currentTextures.add(textureToAdd);

                            if (writeToFile(outputFolder, pokemonObject, statsFile.getName())) {
                                alreadyConvertedPokemon.add(pokemonName);
                                conversions++;
                            } else {
                                logMessage("Couldnt write " + pokemonName + "to file");
                                return false;
                            }
                        }
                    }
                    return conversions > 0;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isForm(String formName, String currentForm, String pokemonName) {
        switch (pokemonName) {
            case "unown":
                return formName.equals("") || formName.equals(currentForm);
            case "tornadus":
                return (formName.equals("") && currentForm.equals("incarnate")) || formName.equals(currentForm);
        }

        return formName.equals(currentForm);
    }

    private static boolean writeToFile(File outputFolder, JsonObject object, String fileName) {
        try {
            File pokemonFile = new File(outputFolder, fileName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(pokemonFile));
            writer.write(gson.toJson(object));
            writer.flush();
            writer.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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

                if (convertedFile.getName().contains(pokemonName + "_") || convertedFile.getName().contains(pokemonName + ".")) {
                    return convertedFile;
                }
            }
        } else {
            for (File statsFile : statsFiles.listFiles()) {
                if (statsFile.isDirectory()) {
                    findPokemonStatsFile(outputFiles, statsFile, pokemonName);
                    continue;
                }

                if (statsFile.getName().contains(pokemonName + "_") || statsFile.getName().contains(pokemonName + ".")) {
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

    private static void logMessage(String message) {
        logMessage(message, false);
    }

    private static void logMessage(String message, boolean error) {
        if (error) {
            System.err.println(message);
        } else {
            System.out.println(message);
        }
        try {
            logWriter.write(message + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void logResult(String message) {
        System.out.println(message);

        try {
            resultWriter.write(message + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private static class EmissiveTextures {
        private File folder;
        private String textureName;

        public EmissiveTextures(File folder, String textureName) {
            this.folder = folder;
            this.textureName = textureName;
        }

        public File getFolder() {
            return folder;
        }

        public String getTextureName() {
            return textureName;
        }
    }
}
