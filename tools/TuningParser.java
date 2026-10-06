package rtmsway;

import java.util.*;

/** JSは実行しない。Previewerのオブジェクトリテラルだけを限定構文で読む。 */
final class TuningParser {
    private final String text;
    private int pos, depth;
    private TuningParser(String text) { this.text = text; }
    static Map<String, Object> parse(String text) {
        if (text.length() > 262144) throw new IllegalArgumentException("設定ファイルが大きすぎます");
        TuningParser p = new TuningParser(text);
        if (p.word().equals("var")) { /* Previewerの出力 */ }
        else throw p.error("var MOTION_TUNING が必要です");
        if (!p.word().equals("MOTION_TUNING")) throw p.error("MOTION_TUNING が必要です");
        p.expect('='); Object root = p.value();
        if (!(root instanceof Map)) throw p.error("設定のルートはオブジェクトです");
        p.space(); if (p.peek() == ';') p.pos++;
        p.space(); if (p.pos != p.text.length()) throw p.error("設定以外のコードは読み込めません");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>)root;
        return result;
    }
    private Object value() {
        space(); if (++depth > 16) throw error("入れ子が深すぎます");
        Object result; char c = peek();
        if (c == '{') {
            pos++; Map<String, Object> map = new LinkedHashMap<String, Object>(); space();
            while (peek() != '}') {
                String key = peek() == '"' || peek() == '\'' ? string() : word();
                expect(':'); if (map.containsKey(key)) throw error("重複キー: " + key);
                map.put(key, value()); space(); if (peek() != ',') break; pos++; space();
            }
            expect('}'); result = map;
        } else if (c == '[') {
            pos++; List<Object> list = new ArrayList<Object>(); space();
            while (peek() != ']') { list.add(value()); space(); if (peek() != ',') break; pos++; space(); }
            expect(']'); result = list;
        } else if (c == '"' || c == '\'') result = string();
        else if (c == '-' || c == '+' || c == '.' || Character.isDigit(c)) {
            int start = pos;
            while (pos < text.length() && "0123456789.eE+-".indexOf(text.charAt(pos)) >= 0) pos++;
            try { double n = Double.parseDouble(text.substring(start, pos)); if (!Double.isFinite(n)) throw error("有限数が必要です"); result = n; }
            catch (NumberFormatException e) { throw error("数値の書式が不正です"); }
        } else {
            String w = word();
            if (w.equals("true")) result = Boolean.TRUE;
            else if (w.equals("false")) result = Boolean.FALSE;
            else if (w.equals("null")) result = null;
            else throw error("式や関数は使用できません: " + w);
        }
        depth--; return result;
    }
    private String word() {
        space(); int start = pos;
        if (!(Character.isLetter(peek()) || peek() == '_' || peek() == '$')) throw error("識別子が必要です");
        while (pos < text.length() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '$')) pos++;
        return text.substring(start, pos);
    }
    private String string() {
        char quote = text.charAt(pos++); StringBuilder out = new StringBuilder();
        while (pos < text.length()) {
            char c = text.charAt(pos++); if (c == quote) return out.toString();
            if (c == '\n' || c == '\r') throw error("文字列内の改行");
            if (c == '\\') {
                if (pos == text.length()) throw error("未完のエスケープ");
                c = text.charAt(pos++);
                if (c == 'n') c = '\n'; else if (c == 'r') c = '\r'; else if (c == 't') c = '\t';
                else if (c == 'u') {
                    if (pos + 4 > text.length()) throw error("未完のUnicodeエスケープ");
                    try { c = (char)Integer.parseInt(text.substring(pos, pos + 4), 16); pos += 4; }
                    catch (NumberFormatException e) { throw error("Unicodeエスケープが不正です"); }
                } else if (c != '\\' && c != '"' && c != '\'' && c != '/') throw error("エスケープが不正です");
            }
            out.append(c);
        }
        throw error("文字列が閉じられていません");
    }
    private char peek() { return pos < text.length() ? text.charAt(pos) : '\0'; }
    private void expect(char c) { space(); if (peek() != c) throw error("'" + c + "' が必要です"); pos++; }
    private void space() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (Character.isWhitespace(c) || c == '\ufeff') { pos++; continue; }
            if (c == '/' && pos + 1 < text.length()) {
                char next = text.charAt(pos + 1);
                if (next == '/') { pos += 2; while (pos < text.length() && text.charAt(pos) != '\n') pos++; continue; }
                if (next == '*') { int end = text.indexOf("*/", pos + 2); if (end < 0) throw error("コメントが閉じられていません"); pos = end + 2; continue; }
            }
            break;
        }
    }
    private IllegalArgumentException error(String message) {
        int line = 1; for (int i = 0; i < pos; i++) if (text.charAt(i) == '\n') line++;
        return new IllegalArgumentException("行 " + line + ": " + message);
    }
}
