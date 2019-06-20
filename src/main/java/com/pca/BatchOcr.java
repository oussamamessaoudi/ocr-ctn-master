package com.pca;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.security.PdfPKCS7;
import net.ricecode.similarity.JaroWinklerStrategy;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;

import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class BatchOcr {

    public static double DOWNSCALE_IMAGE_SIZE = 1;
    public static Map<String, ArrayList> validityMap = new HashMap<>();
    public static Map<String, ArrayList> expectedMap = new HashMap<>();
    public static Map<String, ArrayList> realityMap = new HashMap<>();
    public static ArrayList<String> validityList = new ArrayList<>();
    public static ArrayList<String> invalidityList = new ArrayList<>();
    public static ArrayList<String> needList = new ArrayList<>();
    public static String lastCheckout;
    public static Map<String, Integer> motifs;

    public static void main(String[] args) throws CertificateEncodingException,
            IOException, InterruptedException {
        /*try {
            reader = new BufferedReader(new FileReader(
                    "Export_03_2019.csv"));
            PrintWriter out = new PrintWriter("res2.txt");
            String line = reader.readLine();
            String folder = "";
            String resultFolder = "";
            while (line != null) {
                folder = line.split(";")[0].replaceAll("\\s+", "");
                resultReader = new BufferedReader(new FileReader(
                        "res.txt"));
                String resultLine = resultReader.readLine();
                while (resultLine != null) {
                    if (resultLine.contains("Dossier :")) {
                        resultFolder = resultLine.split(":")[1].replaceAll(
                                "\\s+", "");
                    }
                    if (folder.equals(resultFolder)) {
                        if (resultLine.contains("Dossier invalide")) {
                            out.println(line + "; " + "0" + "; " + " OCR");
                        } else if (resultLine.contains("Dossier valide")) {
                            out.println(line + "; " + "1" + "; " + " OCR");
                        }
                    }
                    resultLine = resultReader.readLine();
                }
                resultReader.close();
                line = reader.readLine();
            }
            reader.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        //System.out.println(new SimpleDateFormat("HH").format(new Date()));
        doBatch();
    }

    public static void doBatch() throws CertificateEncodingException,
            IOException, InterruptedException {
        System.out.println("batch begins");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date();
        PrintStream o = new PrintStream(new File("\\\\LAP-958336PCA\\Users\\u958336\\Desktop\\temps\\"
                + dateFormat.format(date) + ".txt"));
        System.setOut(o);
        File fullFolder = new File("\\\\LAP-958336PCA\\Users\\u958336\\Desktop\\temps\\");
        List<String> validFolders = new ArrayList<String>();
        List<String> validFoldersWithSynthese = new ArrayList<String>();
        List<String> unvalidFolders = new ArrayList<String>();
        List<String> unvalidFoldersExistence = new ArrayList<String>();
        if (fullFolder.isDirectory()) {
            int i =0;
            File[] folders = fullFolder.listFiles();
            for (File folder : folders) {
                i++;
                System.out.println("Dossier : "+i+"/"+folders.length);
                String nowDate = new SimpleDateFormat("HH").format(new Date());
                String nowDateDay = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                //if (Integer.parseInt(nowDate) == 12 && !lastCheckout.equals(nowDateDay)) {
                // generate rapport pdf

                //}
                System.out.println(folder.getName());
                if (folder.isDirectory()) {
                    System.out
                            .println("------------------------------------------");
                    System.out.println("Dossier : " + folder.getName());
                    File cin = new File(folder.getAbsolutePath()
                            + "/cin_recto-verso.pdf.pdf");
                    File convention = new File(folder.getAbsolutePath()
                            + "/contrat_CONVENTION.pdf.pdf");
                    File kyc = new File(folder.getAbsolutePath()
                            + "/contrat_KYC.pdf.pdf");
                    File photo = new File(folder.getAbsolutePath()
                            + "/photo.pdf.pdf");
                    File signature = new File(folder.getAbsolutePath()
                            + "/signature.pdf.pdf");
                    File synthese = new File(folder.getAbsolutePath()
                            + "/Document_synthese.pdf.pdf");

                    if (cin.exists() && convention.exists() && kyc.exists()
                            && photo.exists() && signature.exists()
                            && synthese.exists()) {
                        validFoldersWithSynthese.add(folder.getAbsolutePath());
                    }
                    else{
                        invalidityList.add(folder.getName());
                        realityMap.put(folder.getName(), new ArrayList());
                        expectedMap.put(folder.getName(), new ArrayList());
                        ArrayList value = new ArrayList();
                        value.add("Manque");
                        validityMap.put(folder.getName(), value);
                    }

                    if (cin.exists() && convention.exists() && kyc.exists()
                            && photo.exists() && signature.exists()) {
                        try {
                            boolean checkConventionValidity = checkConventionValidity(convention
                                    .getAbsolutePath());
                            boolean checkKycValidity = checkKycValidity(kyc
                                    .getAbsolutePath());
                            boolean validateConvention = validateConvention(convention
                                    .getAbsolutePath());
                            boolean validateKyc = validateKyc(kyc
                                    .getAbsolutePath());
                            boolean Rapprochement = Rapprochement(
                                    convention.getAbsolutePath(),
                                    kyc.getAbsolutePath());
                            boolean cinDetectImages = detectImages(cin
                                    .getAbsolutePath()) == 2;
                            boolean photoDetectImages = detectImages(photo
                                    .getAbsolutePath()) == 1;
                            boolean specimenDetectImages = detectImages(signature
                                    .getAbsolutePath()) == 1;
                            Map<String, String> infos = getKycData(kyc
                                    .getAbsolutePath());
                            if (!infos.get("compte").substring(0, 5).equals("21111") && !infos.get("compte").substring(0, 5).equals("21330"))
                                continue;
                            System.out
                                    .println("--- L'existence des fichiers : " + true);
                            System.out.println("--- Compte : "
                                    + infos.get("compte"));
                            System.out.println("--- Signature Convention : "
                                    + checkConventionValidity);
                            System.out.println("--- Signature Kyc : "
                                    + checkKycValidity);
                            System.out.println("--- Convention : "
                                    + validateConvention);
                            System.out.println("--- Kyc : " + validateKyc);
                            System.out.println("--- Rapprochement : "
                                    + Rapprochement);
                            System.out.println("--- CIN : " + cinDetectImages);
                            System.out.println("--- Photo : "
                                    + photoDetectImages);
                            System.out.println("--- SPECIMEN : "
                                    + specimenDetectImages);
                            if (checkConventionValidity && checkKycValidity
                                    && validateConvention && validateKyc
                                    && cinDetectImages && photoDetectImages
                                    && specimenDetectImages) {

                                System.out.println("--- INFOS : " + infos);

                                System.out.println(cin.getAbsolutePath());
                                Map<String, String> results = doOcrNew(folder.getAbsolutePath()
                                        .replace('\\', '/'));
                                System.out.println("--- OCR : " + results);
                                Map<String, Double> score = calculateSimilarity(
                                        infos, results, folder.getName());
                                System.out.println("--- SCORE : " + score);

                                if (score.size() >= 8) {
                                    validFolders.add(folder.getAbsolutePath());
                                    validityList.add(folder.getName());
                                    System.out.println("--- Dossier valide");
                                } else {
                                    invalidityList.add(folder.getName());
                                    unvalidFolders
                                            .add(folder.getAbsolutePath());
                                    System.out.println("--- Dossier invalide");
                                }

                            } else {
                                needList.add(folder.getName());
                                unvalidFolders.add(folder.getAbsolutePath());
                                System.out.println("--- Dossier invalide");
                            }
                        } catch (Exception e) {
                            unvalidFolders.add(folder.getAbsolutePath());
                            needList.add(folder.getName());
                            e.printStackTrace();
                            System.out.println("--- Dossier invalide");
                        }
                    } else {
                        System.out
                                .println("--- L'existence des fichiers : " + false);
                        System.out
                                .println("--- L'existence des fichiers CIN : "
                                        + cin.exists());
                        System.out
                                .println("--- L'existence des fichiers CONVENTION : "
                                        + convention.exists());
                        System.out
                                .println("--- L'existence des fichiers KYC : "
                                        + kyc.exists());
                        System.out
                                .println("--- L'existence des fichiers PHOTO : "
                                        + photo.exists());
                        System.out
                                .println("--- L'existence des fichiers SPECIMEN : "
                                        + signature.exists());
                        System.out.println("--- Dossier invalide");
                        unvalidFoldersExistence.add(folder.getAbsolutePath());
                    }
                } else {
                    System.out
                            .println("------------------------------------------");
                    System.out.println(folder.getName() + " is not a folder");
                }
            }
            System.out
                    .println("**********************************************");
            System.out.println("Dossiers avec synthese : "
                    + validFoldersWithSynthese.size());
            System.out.println("Dossiers valide : " + validFolders.size());
            System.out
                    .println("Dossiers non valide : " + unvalidFolders.size());
            System.out
                    .println("Dossiers non valide en terme d'existence des fichiers : "
                            + unvalidFoldersExistence.size());
            System.out
                    .println("**********************************************");
        }
        // generate rapport pdf
        Document document = new Document();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("\\\\LAP-958336PCA\\Users\\u958336\\Desktop\\temps\\rapport.pdf"));
            document.open();

            PdfPTable table = new PdfPTable(5); // 3 columns.
            table.setWidthPercentage(100); //Width 100%
            table.setSpacingBefore(10f); //Space before table
            table.setSpacingAfter(10f); //Space after table

            //Set Column widths
            float[] columnWidths = {1f, 1f, 1f, 1f, 1f};
            table.setWidths(columnWidths);

            PdfPCell cell1 = new PdfPCell(new Paragraph("Numéro du dossier"));
            cell1.setPaddingLeft(10);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);

            PdfPCell cell2 = new PdfPCell(new Paragraph("Validité"));
            cell2.setPaddingLeft(10);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);


            PdfPCell cell3 = new PdfPCell(new Paragraph("Motif d'invalidation"));
            cell3.setPaddingLeft(10);
            cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell3.setVerticalAlignment(Element.ALIGN_MIDDLE);


            PdfPCell cell4 = new PdfPCell(new Paragraph("Données KYC"));
            cell4.setPaddingLeft(10);
            cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell4.setVerticalAlignment(Element.ALIGN_MIDDLE);


            PdfPCell cell5 = new PdfPCell(new Paragraph("Donnée OCR"));
            cell5.setPaddingLeft(10);
            cell5.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell5.setVerticalAlignment(Element.ALIGN_MIDDLE);

            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);
            table.addCell(cell4);
            table.addCell(cell5);

            //To avoid having the cell border and the content overlap, if you are having thick cell borders
            //cell1.setUserBorderPadding(true);
            //cell2.setUserBorderPadding(true);
            //cell3.setUserBorderPadding(true);
            if (validityList.size() > 0)
                for (int i = 0; i < validityList.size(); i++) {
                    PdfPCell cell31 = new PdfPCell(new Paragraph(validityList.get(i)));
                    PdfPCell cell32 = new PdfPCell(new Paragraph("Valide"));
                    PdfPCell cell33 = new PdfPCell(new Paragraph("Motif"));
                    PdfPCell cell34 = new PdfPCell(new Paragraph(""));
                    PdfPCell cell35 = new PdfPCell(new Paragraph(""));
                    table.addCell(cell31);
                    table.addCell(cell32);
                    table.addCell(cell33);
                    table.addCell(cell34);
                    table.addCell(cell35);
                }
            if (invalidityList.size() > 0)
                for (int i = 0; i < invalidityList.size(); i++) {
                    PdfPCell cell31 = new PdfPCell(new Paragraph(invalidityList.get(i)));
                    PdfPCell cell32 = new PdfPCell(new Paragraph("Invalide"));
                    PdfPCell cell33 = new PdfPCell(new Paragraph(validityMap.get(invalidityList.get(i)).toString()));
                    PdfPCell cell34 = new PdfPCell(new Paragraph(expectedMap.get(invalidityList.get(i)).toString()));
                    PdfPCell cell35 = new PdfPCell(new Paragraph(realityMap.get(invalidityList.get(i)).toString()));
                    table.addCell(cell31);
                    table.addCell(cell32);
                    table.addCell(cell33);
                    table.addCell(cell34);
                    table.addCell(cell35);
                }
            if (needList.size() > 0)
                for (int i = 0; i < needList.size(); i++) {
                    PdfPCell cell31 = new PdfPCell(new Paragraph(needList.get(i)));
                    PdfPCell cell32 = new PdfPCell(new Paragraph("Manque"));
                    PdfPCell cell33 = new PdfPCell(new Paragraph("Motif"));

                    PdfPCell cell34 = new PdfPCell(new Paragraph(""));
                    PdfPCell cell35 = new PdfPCell(new Paragraph(""));
                    table.addCell(cell31);
                    table.addCell(cell32);
                    table.addCell(cell33);
                    table.addCell(cell34);
                    table.addCell(cell35);
                }

            document.add(table);

            document.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getNamesOnSignatures(String path)
            throws CertificateEncodingException, IOException {
        StringBuffer sb = new StringBuffer();
        java.security.Security.addProvider(new BouncyCastleProvider());
        PdfReader pdfReader = new PdfReader(path);
        AcroFields fields = pdfReader.getAcroFields();
        List<String> signatureNames = fields.getSignatureNames();
        if (signatureNames.isEmpty()) {
            return "";
        } else {
            for (String name : signatureNames) {
                PdfPKCS7 pk = fields.verifySignature(name);
                Certificate[] certificates = pk.getCertificates();
                for (Certificate cert : certificates) {
                    if (cert instanceof X509Certificate) {
                        X509Certificate x = (X509Certificate) cert;
                        X500Name x500name = new JcaX509CertificateHolder(x)
                                .getSubject();
                        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
                        sb.append(IETFUtils.valueToString(cn.getFirst()
                                .getValue()));
                    }
                }
            }
        }
        return sb
                .toString()
                .replaceAll("[^a-zA-Z]", "")
                .chars()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                        StringBuilder::append).toString();
    }

    public static String getInfoFromConvention(String path) throws IOException {
        PdfReader pdfReader = new PdfReader(path);
        StringBuffer sb = new StringBuffer();
        PdfReaderContentParser parser = new PdfReaderContentParser(pdfReader);
        TextExtractionStrategy strategy;
        strategy = parser.processContent(pdfReader.getNumberOfPages(),
                new SimpleTextExtractionStrategy());
        sb.append(strategy.getResultantText());
        pdfReader.close();
        String[] lines = sb.toString().split("\\n");
        return lines[lines.length - 6]
                .replaceAll("[^a-zA-Z]", "")
                .replaceAll("Mme|Mr|Mlle", "")
                .chars()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                        StringBuilder::append).toString();
    }

    public static String getInfoFromkyc(String path) throws IOException {
        PdfReader pdfReader = new PdfReader(path);
        StringBuffer sb = new StringBuffer();
        PdfReaderContentParser parser = new PdfReaderContentParser(pdfReader);
        TextExtractionStrategy strategy;
        strategy = parser.processContent(pdfReader.getNumberOfPages(),
                new SimpleTextExtractionStrategy());
        sb.append(strategy.getResultantText());
        pdfReader.close();
        String[] lines = sb.toString().split("\\n");
        return lines[lines.length - 2] + " " + lines[lines.length - 1];
    }

    public static boolean checkConventionValidity(String path)
            throws CertificateEncodingException, IOException {
        return getInfoFromConvention(path).equals(getNamesOnSignatures(path));
    }

    public static boolean checkKycValidity(String path)
            throws CertificateEncodingException, IOException {
        String[] kycNames = getInfoFromkyc(path).split(" ");
        String kyc = "";
        for (int i = 0; i < kycNames.length; i++) {
            if (!"Agent:".equals(kycNames[i]) && !"Client:".equals(kycNames[i])
                    && !kycNames[i].matches(".*\\d+.*")) {
                kyc = kyc + kycNames[i];
            }
        }
        kyc = kyc
                .replaceAll("[^a-zA-Z]", "")
                .chars()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                        StringBuilder::append).toString();
        return kyc.equals(getNamesOnSignatures(path));
    }

    public static Map<String, String> getConventionData(String path)
            throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        StringBuffer sb = new StringBuffer();
        PdfReader pdfReader = new PdfReader(path);
        PdfReaderContentParser parser = new PdfReaderContentParser(pdfReader);
        TextExtractionStrategy strategy;
        strategy = parser.processContent(1, new SimpleTextExtractionStrategy());
        sb.append(strategy.getResultantText());
        pdfReader.close();
        String[] lines = sb.toString().split("\\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("cin")) {
                String[] data = lines[i].split("cin");
                map.put("nomprenom", data[0].trim());
                map.put("cin", data[1].trim());
            } else if (lines[i].contains("Adresse principale")) {
                String[] data = lines[i].split(":");
                map.put("adresse", data[1].trim());
            } else if (lines[i].contains("Relevé d'Identité Bancaire")) {
                String[] data = lines[i].split(":");
                map.put("compte", data[1].trim().substring(6));
            } else if (lines[i].contains("Intitulé de Compte")) {
                String[] data = lines[i].split(":");
                map.put("intitulecompte", data[1].trim());
            }
        }
        return map;
    }

    public static Map<String, String> getKycData(String path)
            throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        StringBuffer sb = new StringBuffer();
        PdfReader pdfReader = new PdfReader(path);
        PdfReaderContentParser parser = new PdfReaderContentParser(pdfReader);
        TextExtractionStrategy strategy;
        strategy = parser.processContent(1, new SimpleTextExtractionStrategy());
        sb.append(strategy.getResultantText());
        strategy = parser.processContent(2, new SimpleTextExtractionStrategy());
        sb.append(strategy.getResultantText());
        pdfReader.close();
        String[] lines = sb.toString().split("\\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("N° Compte")) {
                String[] data = lines[i].split(":");
                map.put("compte", data[1].trim());
            } else if (lines[i].contains("Nom:")) {
                String[] data = lines[i].split(":");
                map.put("nom", data[1].trim());
            } else if (lines[i].contains("Prénom:")) {
                String[] data = lines[i].split(":");
                map.put("prenom", data[1].trim());
            } else if (lines[i].contains("N° pièce d'identité:")) {
                String[] data = lines[i].split(":");
                map.put("cin", data[1].trim());
            } else if (lines[i].contains("Nom du père:")) {
                String[] data = lines[i].split(":");
                map.put("pere", data[1].trim());
            } else if (lines[i].contains("Nom de la mère:")) {
                String[] data = lines[i].split(":");
                map.put("mere", data[1].trim());
            } else if (lines[i].contains("Profession:")) {
                String[] data = lines[i].split(":");
                map.put("profession", data[1].trim());
            } else if (lines[i].contains("Libellé profession:")) {
                String[] data = lines[i].split(":");
                map.put("libelleprofession", data[1].trim());
            } else if (lines[i].contains("Agent:")) {
                String[] data = lines[i].split(":");
                map.put("agent", data[1].trim());
            } else if (lines[i].contains("Adresse personnelle: Rue:")) {
                String[] data = lines[i].split(":");
                map.put("addr", data[2].trim());
            } else if (lines[i].contains("Date entretien:")) {
                String[] data = lines[i].split(":");
                map.put("date", data[1].trim().substring(0, 8));
            }
        }
        return map;
    }

    public static boolean validateKyc(String path) throws IOException {
        Map<String, String> map = getKycData(path);
        if (map.size() < 9)
            return false;
        for (Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean validateConvention(String path) throws IOException {
        Map<String, String> map = getConventionData(path);
        if (map.size() < 5)
            return false;
        for (Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean Rapprochement(String convention, String kyc)
            throws IOException {
        Map<String, String> conventionMap = getConventionData(convention);
        Map<String, String> kycMap = getKycData(kyc);
        if (!conventionMap.get("nomprenom").contains(kycMap.get("nom"))
                || !conventionMap.get("nomprenom").contains(
                kycMap.get("prenom")))
            return false;
        if (!conventionMap.get("compte").contains(kycMap.get("compte"))
                && !kycMap.get("compte").contains(
                conventionMap.get("compte").substring(0,
                        conventionMap.get("compte").length() - 2)))
            return false;
        return true;
    }

    public static int detectImages(String path) throws IOException {
        int count = 0;
        PdfReader reader = new PdfReader(path);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        for (int pageNumber = 1; pageNumber <= reader.getNumberOfPages(); pageNumber++) {
            ImageDetector imageDetector = new ImageDetector();
            parser.processContent(pageNumber, imageDetector);
            if (imageDetector.imageFound) {
                count++;
            }
        }
        return count;
    }

    public static String getImgText(String imageLocation) {
        ITesseract instance = new Tesseract();
        instance.setDatapath("C:/Program Files (x86)/Tesseract-OCR/tessdata/");
        // instance.setLanguage("fra");
        try {
            String imgText = instance.doOCR(new File(imageLocation));
            return imgText;
        } catch (TesseractException e) {
            e.printStackTrace();
            return "Error while reading image";
        }
    }

    public static int executeExtract(String path, String savePath)
            throws IOException, InterruptedException {
        int r = 0;
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("python E:/ocr/extract.py --pdf " + path
                + " --save " + savePath);
        r = pr.waitFor();
        return r;
    }

    public static int executePreProcessing(String path, String savePath)
            throws IOException, InterruptedException {
        int r = 0;
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("python E:/ocr/text_detection.py --image " + path
                + " --east E:/ocr/frozen_east_text_detection.pb --result "
                + savePath);
        r = pr.waitFor();
        return r;
    }

    public static List<String> doOcr(String path) {
        List<String> result = new ArrayList<String>();
        File fullFolder = new File(path);
        if (fullFolder.isDirectory()) {
            File[] files = fullFolder.listFiles();
            for (File file : files) {
                result.add(getImgText(file.getAbsolutePath()).replaceAll(
                        "\\r|\\n", "").replaceAll("[^a-zA-Z1-9]", ""));
            }
        }
        return result;
    }

    public static Map<String, String> doOcrNew(String path)
            throws UnsupportedOperationException, IOException {
        Map<String, String> map = new HashMap<String, String>();
        String url = "http://10.29.2.166:8000/Ocr/";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        String json = "{\"path\":\"" + path + "\"}";
        StringEntity entity = new StringEntity(json);
        post.setEntity(entity);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");
        HttpResponse response = client.execute(post);
        BufferedReader rd = new BufferedReader(new InputStreamReader(response
                .getEntity().getContent()));
        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        client.close();
        JSONObject obj = new JSONObject(result.toString());
        if (obj.getJSONObject("prediction").has("Nom"))
            map.put("nom", obj.getJSONObject("prediction").getString("Nom"));
        if (obj.getJSONObject("prediction").has("Prenom"))
            map.put("prenom",
                    obj.getJSONObject("prediction").getString("Prenom"));
        if (obj.getJSONObject("prediction").has("CIN"))
            map.put("cin", obj.getJSONObject("prediction").getString("CIN"));
        if (obj.getJSONObject("prediction").has("Date de naissance"))
            map.put("birthday",
                    obj.getJSONObject("prediction").getString(
                            "Date de naissance"));
        if (obj.getJSONObject("prediction").has("Date expiration"))
            map.put("expirydate",
                    obj.getJSONObject("prediction").getString(
                            "Date expiration"));
        if (obj.getJSONObject("prediction").has("Adresse"))
            map.put("addr",
                    obj.getJSONObject("prediction").getString("Adresse"));
        if (obj.getJSONObject("prediction").has("Nom Pere"))
            map.put("pere",
                    obj.getJSONObject("prediction").getString("Nom Pere"));
        if (obj.getJSONObject("prediction").has("Nom Mere"))
            map.put("mere",
                    obj.getJSONObject("prediction").getString("Nom Mere"));
        if (obj.has("specimen_signature"))
            map.put("signature",
                    obj.getString("specimen_signature"));
        return map;
    }

    public static Map<String, Double> calculateSimilarity(
            Map<String, String> doc, Map<String, String> ocr, String folderName) throws ParseException {
        Map<String, Double> map = new HashMap<String, Double>();
        double score = 0;
        ArrayList<String> motifList = new ArrayList<>();
        ArrayList<String> reality = new ArrayList<>();
        ArrayList<String> expectation = new ArrayList<>();
        SimilarityStrategy strategy = new JaroWinklerStrategy();
        StringSimilarityService service = new StringSimilarityServiceImpl(
                strategy);

        // CIN
        if (ocr.containsKey("cin")) {
            if (ocr.get("cin").trim().toUpperCase().contains(doc.get("cin").toUpperCase().trim()))
                map.put("cin => " + doc.get("cin") + ":" + ocr.get("cin"),
                        score);
            else {
                motifList.add("Discordance concernant le numéro de la CIN de titulaire au niveau de la demande d’ouverture de compte par référence à la CIN");
                reality.add(ocr.get("cin"));
                expectation.add(doc.get("cin"));
            }
        } else {
            motifList.add("cin illisible");
        }

        // nom
        if (ocr.containsKey("nom")) {
            //double scoreSimilarity = service.score(, ));
            String nom = doc.get("nom");
            if (Arrays.stream(nom.split(" ")).allMatch(s -> ocr.get("nom").contains(s)))
                map.put("nom => " + nom + ":" + ocr.get("nom"),
                        score);
            else {
                motifList.add("Discordance concernant le nom de titulaire au niveau de la demande d’ouverture de compte par référence à la CIN");
                reality.add(ocr.get("nom"));
                expectation.add(nom);
            }
        } else {
            motifList.add("cin illisible");
        }

        // prenom
        if (ocr.containsKey("prenom")) {
            //double scoreSimilarity = service.score(ocr.get("prenom"), doc.get("prenom"));
            String prenom = doc.get("prenom");
            if (Arrays.stream(prenom.split(" ")).allMatch(s -> ocr.get("prenom").contains(s)))
                map.put("prenom => " + prenom + ":" + ocr.get("prenom"),
                        score);
            else {
                motifList.add("Discordance concernant le prénom de titulaire au niveau de la demande d’ouverture de compte par référence à la CIN");
                reality.add(ocr.get("prenom"));
                expectation.add(prenom);
            }
        } else {
            motifList.add("cin illisible");
        }

        // addr
        if (ocr.containsKey("addr")) {
            double scoreSimilarity = 0.9;
            String[] addrFragmentsDoc = doc.get("addr").split(" ");
            String[] addrFragmentsOcr = ocr.get("addr").split(" ");
            for (String fragmentDoc : addrFragmentsDoc) {
                double scoreLocal = 0;
                for (String fragmentOcr : addrFragmentsOcr) {
                    scoreLocal = Math.max(service.score(fragmentDoc, fragmentOcr), scoreLocal);
                }
                if (scoreLocal <= 0.8) {
                    scoreSimilarity = score;
                    break;
                }

            }
            if (scoreSimilarity > 0.8)
                map.put("addr => " + doc.get("addr") + ":" + ocr.get("addr"),
                        score);
            else {
                motifList.add("Discordance concernant l'adresse de titulaire au niveau de la demande d’ouverture de compte par référence à la CIN");
                reality.add(ocr.get("addr"));
                expectation.add(doc.get("addr"));
            }
        } else {
            motifList.add("cin illisible");
        }


        if (ocr.containsKey("signature")) {
            if (ocr.get("signature").equals("Oui")) {
                map.put("specimen => ",
                        1.0);
            } else {
                motifList.add("Carton spécimen ne contient pas la signature");
            }
        } else {
            motifList.add("Carton spécimen de signature illisible");
        }
        // nom
        if (ocr.containsKey("mere")) {
            //double scoreSimilarity = service.score(ocr.get("mere"), doc.get("mere"));
            String patternStr = " BEN | BENT | BNT ";
            String[] motherNames = doc.get("mere").split(patternStr);
            String motherName = null;
            if (motherNames.length > 1) {
                motherName = motherNames[0];
            } else {
                motherNames = doc.get("mere").split(" ");
            }
            if (motherName != null && !motherName.isEmpty() && Arrays.stream(motherName.split(" ")).allMatch(s -> ocr.get("mere").contains(s)))
                map.put("mere => " + doc.get("mere") + ":" + ocr.get("mere"),
                        score);
            else if (motherName == null) {
                score = 0;
                for (String motherNameLoc : motherNames) {
                    if (ocr.get("mere").contains(motherNameLoc)) score++;
                }
                if (score >= ((double) motherNames.length) / 2) {
                    map.put("mere => " + doc.get("mere") + ":" + ocr.get("mere"),
                            score);
                } else {
                    motifList.add("Discordance concernant le nom et /ou le prénom de la mère sur le compte rendu d’entretien du titulaire du compte par référence à la CIN");
                    reality.add(ocr.get("mere"));
                    expectation.add(doc.get("mere"));
                }
            } else {
                motifList.add("Discordance concernant le nom et /ou le prénom de la mère sur le compte rendu d’entretien du titulaire du compte par référence à la CIN");
                reality.add(ocr.get("mere"));
                expectation.add(doc.get("mere"));
            }
        } else {
            motifList.add("cin illisible");
        }

        // prenom
        if (ocr.containsKey("pere")) {
            double scoreSimilarity = service.score(ocr.get("pere"), doc.get("pere"));
            int index = doc.get("pere").indexOf(" BEN ");
            String fatherNameDoc = null;
            String[] fatherNames = null;
            if (index != -1) {
                fatherNameDoc = doc.get("pere").substring(0, index).trim();
            } else {
                fatherNames = doc.get("pere").split(" ");
            }


            if (fatherNameDoc != null && !fatherNameDoc.isEmpty() && Arrays.stream(fatherNameDoc.split(" ")).allMatch(s -> ocr.get("pere").contains(s))) {
                map.put("pere => " + doc.get("pere") + ":" + ocr.get("pere"),
                        score);
            } else if (fatherNames != null && fatherNames.length > 0) {
                score = 0;
                for (String fatherName : fatherNames) {
                    if (ocr.get("pere").contains(fatherName)) score++;
                }
                if (score >= ((double) fatherNames.length) / 2) {
                    map.put("pere => " + doc.get("pere") + ":" + ocr.get("pere"),
                            score);
                } else {
                    motifList.add("Discordance concernant le nom et /ou le prénom du père sur le compte rendu d’entretien du titulaire du compte par référence à la CIN");
                    reality.add(ocr.get("pere"));
                    expectation.add(doc.get("pere"));
                }
            } else {
                motifList.add("Discordance concernant le nom et /ou le prénom du père sur le compte rendu d’entretien du titulaire du compte par référence à la CIN");
                reality.add(ocr.get("pere"));
                expectation.add(doc.get("pere"));
            }
        } else {
            motifList.add("cin illisible");
        }
        //expery date
        if (ocr.containsKey("expirydate")) {
            List<String> noms = Arrays.asList(ocr.get("expirydate").trim().toLowerCase().split(" "));
            String docNoms = doc.get("date").trim();
            Date date1 = new SimpleDateFormat("dd-MM-yy").parse(docNoms);
            int c = 0;
            if (noms.size() == 3) {
                String date = noms.get(2) + noms.get(1) + noms.get(0);
                Date date2 = new SimpleDateFormat("ddMMyy").parse(date);
                if (date1.compareTo(date2) < 0) {
                    map.put("date => " + doc.get("date") + ":" + ocr.get("expirydate"),
                            score);
                } else {
                    motifList.add("Expiration de la date de validité de la copie de la carte d’identité nationale marocaine du titulaire de compte");
                    reality.add(ocr.get("expirydate"));
                    expectation.add(doc.get("date"));
                }
            }
        } else {
            motifList.add("cin illisible");
        }

        realityMap.put(folderName, reality);
        expectedMap.put(folderName, expectation);
        validityMap.put(folderName, motifList);
        return map;
    }
}
