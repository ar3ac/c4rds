val html = """<img src="https://images.seeklogo.com/logo-png/35/2/media-world-logo-png_seeklogo-359798.png" alt="Media World Logo PNG Vector" />"""
val regex = """src="(https://images\.seeklogo\.com/[^"]+)"[^>]*alt="([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)
println(regex.findAll(html).count())
