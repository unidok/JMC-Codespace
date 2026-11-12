package me.unidok.jmccodespace.uploader

interface ModuleUploader {
    /**
     * @param data Модуль в виде JSON
     * @return Ссылка на загрузку модуля
     */
    fun upload(data: String): String
}