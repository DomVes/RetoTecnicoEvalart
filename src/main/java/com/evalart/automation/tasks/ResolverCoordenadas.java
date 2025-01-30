package com.evalart.automation.tasks;

import com.evalart.automation.userinterface.AutomatizarCoordenadasUI;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;

public class ResolverCoordenadas implements Question<int[]> {

    @Override
    public int[] answeredBy(Actor actor) {
        String ecuacion = AutomatizarCoordenadasUI.ECUACION_TEXT.resolveFor(actor).getText();
        System.out.println("Texto extraído: " + ecuacion);

        Pattern pattern = Pattern.compile("\\(([-]?\\d+),([-]?\\d+)\\)");
        Matcher matcher = pattern.matcher(ecuacion);
        List<String[]> coordenadasList = new ArrayList<>();

        while (matcher.find()) {
            String[] coordenada = new String[]{matcher.group(1), matcher.group(2)};
            coordenadasList.add(coordenada);
        }

        int sumaX = 0;
        int sumaY = 0;

        for (String[] valores : coordenadasList) {
            try {
                int x = Integer.parseInt(valores[0].trim());
                int y = Integer.parseInt(valores[1].trim());

                sumaX += x;
                sumaY += y;
                
                System.out.println("Coordenada procesada: (" + x + "," + y + ")");
            } catch (NumberFormatException e) {
                System.out.println("Error al procesar coordenada: (" + valores[0] + "," + valores[1] + ")");
                throw e;
            }
        }

        System.out.println("Valor de X: " + sumaX);
        System.out.println("Valor de Y: " + sumaY);

        return new int[]{sumaX, sumaY};
    }

    public static ResolverCoordenadas enLaPagina() {
        return new ResolverCoordenadas();
    }
}
