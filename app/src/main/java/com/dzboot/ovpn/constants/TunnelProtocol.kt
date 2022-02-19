package com.dzboot.ovpn.constants

class TunnelProtocol {
    private lateinit var type: Tunnel
    private val dnsTT = "Dns TT"
    private val fastDns = "Fast Dns"
    private val slowDns = "Slow Dns"
    private val udpMode = "UDP Mode"

    enum class Tunnel {
        DNS_TT,
        FAST_DNS,
        SLOW_DNS,
        UDP_MODE,
        NOT_ALLOWED
    }

    fun getName():String {
        return when(this.type) {
            Tunnel.DNS_TT -> dnsTT
            Tunnel.FAST_DNS -> fastDns
            Tunnel.SLOW_DNS -> slowDns
            Tunnel.UDP_MODE -> udpMode
            Tunnel.NOT_ALLOWED -> "Not Allowed"
        }
    }

    fun setType(name:String) {
        this.type = when(name) {
            dnsTT -> Tunnel.DNS_TT
            fastDns -> Tunnel.FAST_DNS
            slowDns -> Tunnel.SLOW_DNS
            udpMode -> Tunnel.UDP_MODE
            else -> Tunnel.NOT_ALLOWED
        }
    }

    fun getType(): Tunnel { return type }

    fun setType(type: Tunnel) { this.type = type }
}