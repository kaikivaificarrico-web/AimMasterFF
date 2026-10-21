package com.aimmaster.ff.training

enum class DeviceClass(val label: String, val offset: Int) {
    BASIC("Celular de entrada", -5),
    MID("Celular intermediário", 0),
    HIGH("Topo de linha (90/120 Hz)", 0),
    TABLET("Tablet / tela grande", 5)
}

enum class PlayStyle(val label: String, val base: IntArray) {
    // ordem: Geral, Ponto vermelho, Mira 2x, Mira 4x, AWM, Olhar livre
    RUSH("Rushador (curta distância)", intArrayOf(95, 92, 88, 82, 70, 70)),
    BALANCED("Equilibrado", intArrayOf(88, 84, 80, 75, 65, 65)),
    CAPA("Capa / puxada rápida", intArrayOf(100, 95, 90, 85, 72, 70)),
    SNIPER("Sniper (longa distância)", intArrayOf(78, 75, 75, 72, 55, 60))
}

data class SensitivityResult(val items: List<Pair<String, Int>>)

object SensitivityCalculator {
    val labels = listOf("Geral", "Ponto vermelho", "Mira 2x", "Mira 4x", "AWM", "Olhar livre")

    fun calculate(device: DeviceClass, style: PlayStyle, fine: Int): SensitivityResult {
        val list = labels.indices.map { i ->
            labels[i] to (style.base[i] + device.offset + fine).coerceIn(1, 100)
        }
        return SensitivityResult(list)
    }

    /** Dica simples baseada nos últimos treinos de capa. */
    fun hintFrom(results: List<SessionResult>): String {
        val flicks = results.filter { it.mode == TrainingMode.FLICK.name }.takeLast(5)
        if (flicks.size < 2) {
            return "Faça alguns treinos de capa para receber dicas de ajuste baseadas no seu desempenho."
        }
        val acc = flicks.map { it.accuracy }.average()
        val pull = flicks.map { it.avgPullMs }.filter { it > 0 }.average()
        return when {
            acc < 40 && pull < 200 ->
                "Você puxa rápido mas erra a cabeça com frequência. Tente reduzir 3 a 5 pontos nas sensibilidades de mira."
            acc >= 60 && pull > 300 ->
                "Você acerta bem, mas a puxada está lenta. Tente aumentar 3 a 5 pontos para ganhar velocidade."
            else ->
                "Seu desempenho está equilibrado. Mantenha os valores e mude só 1 ou 2 pontos por vez, testando por alguns dias."
        }
    }
}
