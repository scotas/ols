package org.apache.lucene.analysis;

import com.scotas.lucene.indexer.LuceneDomainIndex;

import java.io.Reader;

import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;


/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * A Multi language analyzer implementation. The included languages are: portuguese, english,
 * spanish, dutch, italian, french and german.
 *
 */

public class MultilingualAnalyzer extends Analyzer {

    private CharArraySet stopWords;

    /**
     * An array containing some common words that are not usually useful for searching. see:
     * http://snowball.tartarus.org/algorithms/portuguese/stop.txt
     * http://snowball.tartarus.org/algorithms/english/stop.txt
     * http://snowball.tartarus.org/algorithms/spanish/stop.txt
     * http://snowball.tartarus.org/algorithms/dutch/stop.txt
     * http://snowball.tartarus.org/algorithms/italian/stop.txt
     * http://snowball.tartarus.org/algorithms/french/stop.txt
     * http://snowball.tartarus.org/algorithms/german/stop.txt
     *
     * Remove duplicated entries: "me", "es", "de", "era", "se", "a", "in", "tu", "esta", "te",
     * "que", "da", "la", "e", "no", "nos", "estamos", "un", "ha", "fosse", "son", "as", "o", "al",
     * "en", "fui", "was", "le", "sera", "of", "sua", "will", "mi", "ai", "na", "del", "andere",
     * "he", "y", "mais", "alles", "uno", "um", "eramos", "doch", "entre", "esta", "a", "all",
     * "vos", "des", "estas", "am", "ser", "die", "il", "i", "do", "je", "so", "for", "ou", "ne",
     * "para", "tua", "por", "das", "ma", "on", "somos", "waren", "como", "also", "sois", "over",
     * "is", "os", "c", "lo", "eu", "an", "der", "er", "als", "l", "alle", "ti", "una", "seriamos",
     * "du", "has", "seremos", "con", "hier", "este", "su", "wie", "tenho", "lui", "had",
     */

    private static final String[] MULTI_STOP_WORDS = {
        /*
     * Portuguese stop words begin
     */
        "de", "a", "o", "que", "e", "do", "da", "em", "um", "para", "e", "com",
        "nao", "uma", "os", "no", "se", "na", "por", "mais", "as", "dos",
        "como", "mas", "foi", "ao", "ele", "das", "tem", "a", "seu", "sua",
        "ou", "ser", "quando", "muito", "ha", "nos", "ja", "esta", "eu",
        "tambem", "so", "pelo", "pela", "ate", "isso", "ela", "entre", "era",
        "depois", "sem", "mesmo", "aos", "ter", "seus", "quem", "nas", "meu",
        "esse", "eles", "estao", "voce", "tinha", "foram", "essa", "num",
        "nem", "suas", "as", "minha", "tem", "numa", "pelos", "elas", "havia",
        "seja", "qual", "sera", "nos", "tenho", "lhe", "deles", "essas",
        "esses", "pelas", "este", "fosse", "dele", "tu", "te", "voces", "vos",
        "lhes", "meus", "minhas", "teu", "tua", "teus", "tuas", "nosso",
        "nossa", "nossos", "nossas", "dela", "delas", "esta", "estes", "estas",
        "aquele", "aquela", "aqueles", "aquelas", "isto", "aquilo", "estou",
        "estamos", "estive", "esteve", "estivemos", "estiveram", "estava",
        "estavamos", "estavam", "estivera", "estiveramos", "esteja",
        "estejamos", "estejam", "estivesse", "estivessemos", "estivessem",
        "estiver", "estivermos", "estiverem", "hei", "havemos", "hao", "houve",
        "houvemos", "houveram", "houvera", "houveramos", "haja", "hajamos",
        "hajam", "houvesse", "houvessemos", "houvessem", "houver", "houvermos",
        "houverem", "houverei", "houvera", "houveremos", "houverao",
        "houveria", "houveriamos", "houveriam", "sou", "somos", "sao",
        "eramos", "eram", "fui", "fomos", "fora", "foramos", "sejamos",
        "sejam", "fossemos", "fossem", "for", "formos", "forem", "serei",
        "seremos", "serao", "seria", "seriamos", "seriam", "temos", "tem",
        "tinhamos", "tinham", "tive", "teve", "tivemos", "tiveram", "tivera",
        "tiveramos", "tenha", "tenhamos", "tenham", "tivesse", "tivessemos",
        "tivessem", "tiver", "tivermos", "tiverem", "terei", "tera", "teremos",
        "terao", "teria", "teriamos", "teriam",
        /*
     * English stop words - begin
     */
        "i", "me", "my", "myself", "we", "us", "our", "ours", "ourselves",
        "you", "your", "yours", "yourself", "yourselves", "he", "him", "his",
        "himself", "she", "her", "hers", "herself", "it", "its", "itself",
        "they", "them", "their", "theirs", "themselves", "what", "which",
        "who", "whom", "this", "that", "these", "those", "am", "is", "are",
        "was", "were", "be", "been", "being", "have", "has", "had", "having",
        "does", "did", "doing", "will", "would", "shall", "should", "can",
        "could", "may", "might", "must", "ought", "i", " m", "you", "re", "he",
        "s", "she", "it", "we", "they", "ve", "you", "we", "they", "d", "ll",
        "isn", "t", "aren", "wasn", "weren", "hasn", "haven", "hadn", "doesn",
        "don", "didn", "won", "wouldn", "shan", "shouldn", "cannot", "couldn",
        "mustn", "let", "here", "there", "when", "where", "why", "how", "an",
        "the", "and", "but", "if", "or", "because", "until", "while", "of",
        "at", "by", "with", "about", "against", "between", "into", "through",
        "during", "before", "after", "above", "below", "to", "from", "up",
        "down", "in", "out", "on", "off", "over", "under", "again", "further",
        "then", "once", "here", "there", "when", "where", "why", "how", "all",
        "any", "both", "each", "few", "more", "most", "other", "some", "such",
        "nor", "not", "only", "own", "same", "so", "than", "too", "very",
        "one", "every", "least", "less", "many", "now", "ever", "never", "say",
        "says", "said", "also", "get", "go", "goes", "just", "made", "make",
        "put", "see", "seen", "whether", "like", "well", "back", "even",
        "still", "way", "take", "since", "another", "however", "two", "three",
        "four", "five", "first", "second", "new", "old", "high", "long",
        /*
		 * Spanish stop words begin
		 */
        "la", "el", "en", "y", "los", "del", "las", "un", "con", "una", "su",
        "al", "es", "lo", "mas", "pero", "sus", "le", "ya", "fue", "ha", "si",
        "porque", "son", "cuando", "muy", "sin", "sobre", "tiene", "tambien",
        "hasta", "hay", "donde", "han", "quien", "estan", "estado", "desde",
        "todo", "durante", "estados", "todos", "uno", "les", "ni", "contra",
        "otros", "fueron", "ese", "eso", "habia", "ante", "ellos", "esto",
        "mi", "antes", "algunos", "que", "unos", "yo", "otro", "otras", "otra",
        "el", "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos",
        "cual", "sea", "poco", "ella", "estar", "haber", "estaba", "algunas",
        "algo", "nosotros", "mi", "mis", "tu", "ti", "tus", "ellas",
        "nosotras", "vosotros", "vosotras", "mio", "mia", "mios", "mias",
        "tuyo", "tuya", "tuyos", "tuyas", "suyo", "suya", "suyos", "suyas",
        "nuestro", "nuestra", "nuestros", "nuestras", "vuestro", "vuestra",
        "vuestros", "vuestras", "esos", "esas", "estoy", "estas", "estais",
        "este", "estes", "estemos", "esteis", "esten", "estare", "estaras",
        "estara", "estaremos", "estareis", "estaran", "estaria", "estarias",
        "estariamos", "estariais", "estarian", "estabas", "estabamos",
        "estabais", "estaban", "estuve", "estuviste", "estuvo", "estuvimos",
        "estuvisteis", "estuvieron", "estuviera", "estuvieras", "estuvieramos",
        "estuvierais", "estuvieran", "estuviese", "estuvieses", "estuviesemos",
        "estuvieseis", "estuviesen", "estando", "estada", "estadas", "estad",
        "hemos", "habeis", "haya", "hayas", "hayamos", "hayais", "hayan",
        "habre", "habras", "habra", "habremos", "habreis", "habran", "habria",
        "habrias", "habriamos", "habriais", "habrian", "habias", "habiamos",
        "habiais", "habian", "hube", "hubiste", "hubo", "hubimos", "hubisteis",
        "hubieron", "hubiera", "hubieras", "hubieramos", "hubierais",
        "hubieran", "hubiese", "hubieses", "hubiesemos", "hubieseis",
        "hubiesen", "habiendo", "habido", "habida", "habidos", "habidas",
        "soy", "eres", "sois", "seas", "seamos", "seais", "sean", "sere",
        "seras", "sereis", "seran", "seria", "serias", "seriais", "serian",
        "eras", "erais", "eran", "fuiste", "fuimos", "fuisteis", "fuera",
        "fueras", "fueramos", "fuerais", "fueran", "fuese", "fueses",
        "fuesemos", "fueseis", "fuesen", "siendo", "sido", "tengo", "tienes",
        "tenemos", "teneis", "tienen", "tenga", "tengas", "tengamos",
        "tengais", "tengan", "tendre", "tendras", "tendra", "tendremos",
        "tendreis", "tendran", "tendria", "tendrias", "tendriamos",
        "tendriais", "tendrian", "tenia", "tenias", "teniamos", "teniais",
        "tenian", "tuve", "tuviste", "tuvo", "tuvimos", "tuvisteis",
        "tuvieron", "tuviera", "tuvieras", "tuvieramos", "tuvierais",
        "tuvieran", "tuviese", "tuvieses", "tuviesemos", "tuvieseis",
        "tuviesen", "teniendo", "tenido", "tenida", "tenidos", "tenidas",
        "tened",
        /*
		 * Dutch stop words begin
		 */
        "van", "ik", "dat", "die", "een", "hij", "het", "niet", "zijn", "op",
        "aan", "met", "als", "voor", "er", "maar", "om", "hem", "dan", "zou",
        "wat", "mijn", "men", "dit", "zo", "door", "ze", "zich", "bij", "ook",
        "tot", "je", "mij", "uit", "der", "daar", "haar", "naar", "heb", "hoe",
        "heeft", "hebben", "deze", "u", "want", "nog", "zal", "zij", "nu",
        "ge", "geen", "omdat", "iets", "worden", "toch", "waren", "veel",
        "meer", "doen", "toen", "moet", "ben", "zonder", "kan", "hun", "dus",
        "alles", "onder", "ja", "eens", "hier", "wie", "werd", "altijd",
        "doch", "wordt", "wezen", "kunnen", "ons", "zelf", "tegen", "reeds",
        "wil", "kon", "niets", "uw", "iemand", "geweest", "andere",
        /*
     * Italian stop words begim
     */
        "ad", "allo", "ai", "agli", "agl", "alla", "alle", "col", "coi", "dal",
        "dallo", "dai", "dagli", "dall", "dagl", "dalla", "dalle", "di",
        "dello", "dei", "degli", "dell", "degl", "della", "delle", "nel",
        "nello", "nei", "negli", "nell", "negl", "nella", "nelle", "sul",
        "sullo", "sui", "sugli", "sull", "sugl", "sulla", "sulle", "per",
        "tra", "contro", "io", "lui", "lei", "noi", "voi", "loro", "mio",
        "mia", "miei", "mie", "tuo", "tuoi", "tue", "suo", "suoi", "sue",
        "nostro", "nostra", "nostri", "nostre", "vostro", "vostra", "vostri",
        "vostre", "ci", "vi", "li", "gli", "ne", "il", "ma", "ed", "perche",
        "anche", "come", "dov", "dove", "che", "chi", "cui", "non", "piu",
        "quale", "quanto", "quanti", "quanta", "quante", "quello", "quelli",
        "quella", "quelle", "questo", "questi", "questa", "queste", "si",
        "tutto", "tutti", "c", "l", "ho", "hai", "abbiamo", "avete", "hanno",
        "abbia", "abbiate", "abbiano", "avro", "avrai", "avra", "avremo",
        "avrete", "avranno", "avrei", "avresti", "avrebbe", "avremmo",
        "avreste", "avrebbero", "avevo", "avevi", "aveva", "avevamo",
        "avevate", "avevano", "ebbi", "avesti", "ebbe", "avemmo", "aveste",
        "ebbero", "avessi", "avesse", "avessimo", "avessero", "avendo",
        "avuto", "avuta", "avuti", "avute", "sono", "sei", "e", "siamo",
        "siete", "sia", "siate", "siano", "saro", "sarai", "sara", "saremo",
        "sarete", "saranno", "sarei", "saresti", "sarebbe", "saremmo",
        "sareste", "sarebbero", "ero", "eri", "eravamo", "eravate", "erano",
        "fosti", "fu", "fummo", "foste", "furono", "fossi", "fossimo",
        "fossero", "essendo", "faccio", "fai", "facciamo", "fanno", "faccia",
        "facciate", "facciano", "faro", "farai", "fara", "faremo", "farete",
        "faranno", "farei", "faresti", "farebbe", "faremmo", "fareste",
        "farebbero", "facevo", "facevi", "faceva", "facevamo", "facevate",
        "facevano", "feci", "facesti", "fece", "facemmo", "faceste", "fecero",
        "facessi", "facesse", "facessimo", "facessero", "facendo", "sto",
        "stai", "sta", "stiamo", "stanno", "stia", "stiate", "stiano", "staro",
        "starai", "stara", "staremo", "starete", "staranno", "starei",
        "staresti", "starebbe", "staremmo", "stareste", "starebbero", "stavo",
        "stavi", "stava", "stavamo", "stavate", "stavano", "stetti", "stesti",
        "stette", "stemmo", "steste", "stettero", "stessi", "stesse",
        "stessimo", "stessero", "stando",
        /*
		 * French stop words begin
		 */
        "au", "aux", "avec", "ce", "ces", "dans", "des", "du", "elle", "et",
        "eux", "leur", "meme", "mes", "moi", "mon", "notre", "nous", "par",
        "pas", "pour", "qu", "qui", "sa", "ses", "sur", "ta", "tes", "toi",
        "ton", "une", "votre", "vous", "d", "j", "m", "n", "s", "t", "ete",
        "etee", "etees", "etes", "etant", "etante", "etants", "etantes",
        "suis", "est", "sommes", "etes", "sont", "serai", "seras", "sera",
        "serons", "serez", "seront", "serais", "serait", "serions", "seriez",
        "seraient", "etais", "etait", "etions", "etiez", "etaient", "fus",
        "fut", "fumes", "futes", "furent", "soit", "soyons", "soyez", "soient",
        "fusse", "fusses", "fut", "fussions", "fussiez", "fussent", "ayant",
        "ayante", "ayantes", "ayants", "eue", "eues", "eus", "avons", "avez",
        "ont", "aurai", "auras", "aura", "aurons", "aurez", "auront", "aurais",
        "aurait", "aurions", "auriez", "auraient", "avais", "avait", "avions",
        "aviez", "avaient", "eut", "eumes", "eutes", "eurent", "aie", "aies",
        "ait", "ayons", "ayez", "aient", "eusse", "eusses", "eut", "eussions",
        "eussiez", "eussent",
        /*
     * German stop words begin
     */
        "aber", "allem", "allen", "aller", "ander", "anderem", "anderen",
        "anderer", "anderes", "anderm", "andern", "anderr", "anders", "auch",
        "auf", "aus", "bei", "bin", "bis", "bist", "damit", "dann", "den",
        "dem", "da", "derselbe", "derselben", "denselben", "desselben",
        "demselben", "dieselbe", "dieselben", "dasselbe", "dazu", "dein",
        "deine", "deinem", "deinen", "deiner", "deines", "denn", "derer",
        "dessen", "dich", "dir", "dies", "diese", "diesem", "diesen", "dieser",
        "dieses", "dort", "durch", "ein", "eine", "einem", "einen", "einer",
        "eines", "einig", "einige", "einigem", "einigen", "einiger", "einiges",
        "einmal", "ihn", "ihm", "etwas", "euer", "eure", "eurem", "euren",
        "eurer", "eures", "fur", "gegen", "gewesen", "hab", "habe", "haben",
        "hat", "hatte", "hatten", "hin", "hinter", "ich", "mich", "mir", "ihr",
        "ihre", "ihrem", "ihren", "ihrer", "ihres", "euch", "im", "indem",
        "ins", "ist", "jede", "jedem", "jeden", "jeder", "jedes", "jene",
        "jenem", "jenen", "jener", "jenes", "jetzt", "kann", "kein", "keine",
        "keinem", "keinen", "keiner", "keines", "konnen", "konnte", "machen",
        "man", "manche", "manchem", "manchen", "mancher", "manches", "mein",
        "meine", "meinem", "meinen", "meiner", "meines", "mit", "muss",
        "musste", "nach", "nicht", "nichts", "noch", "nun", "nur", "ob",
        "oder", "ohne", "sehr", "sein", "seine", "seinem", "seinen", "seiner",
        "seines", "selbst", "sich", "sie", "ihnen", "sind", "solche",
        "solchem", "solchen", "solcher", "solches", "soll", "sollte",
        "sondern", "sonst", "uber", "und", "uns", "unse", "unsem", "unsen",
        "unser", "unses", "unter", "viel", "vom", "von", "vor", "w�hrend",
        "war", "warst", "weg", "weil", "weiter", "welche", "welchem",
        "welchen", "welcher", "welches", "wenn", "werde", "werden", "wieder",
        "wir", "wird", "wirst", "wo", "wollen", "wollte", "wurde", "wurden",
        "zu", "zum", "zur", "zwar", "zwischen" };

    public MultilingualAnalyzer() {

        stopWords =
                StopFilter.makeStopSet(Version.LUCENE_40, MULTI_STOP_WORDS);
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName,
                                                              Reader reader) {
        if (fieldName == null)
            throw new IllegalArgumentException("fieldName must not be null");
        if (reader == null)
            throw new IllegalArgumentException("reader must not be null");

        Tokenizer tokenizer =
            new StandardTokenizer(LuceneDomainIndex.LUCENE_COMPAT_VERSION,
                                  reader);
        TokenStream result =
            new StandardFilter(LuceneDomainIndex.LUCENE_COMPAT_VERSION,
                               tokenizer);

        //Remove accentuation
        result = new ASCIIFoldingFilter(result);

        // Convert to lower case
        result =
                new LowerCaseFilter(LuceneDomainIndex.LUCENE_COMPAT_VERSION, result);

        // Remove stop words
        result =
                new StopFilter(LuceneDomainIndex.LUCENE_COMPAT_VERSION, result,
                               stopWords);
        return new TokenStreamComponents(tokenizer, result);
    }
}
